use crate::collection::CollectionManager;
use anyhow::{anyhow, Context, Result};
use chrono::{Duration, Utc};
use lab7_shared::{Address, Coordinates, Organization, OrganizationData, OrganizationType};
use postgres::{Client, NoTls, Row};
use std::cmp::Ordering;
use std::collections::HashMap;
use std::sync::{Arc, RwLock};

pub struct Database {
    url: String,
    user: String,
    password: String,
    collection: Arc<RwLock<CollectionManager>>,
}

impl Database {
    pub fn new(
        env: &HashMap<String, String>,
        collection: Arc<RwLock<CollectionManager>>,
    ) -> Result<Self> {
        Ok(Self {
            url: env
                .get("URL")
                .context("url for db should be specified in env")?
                .trim_start_matches("jdbc:")
                .to_string(),
            user: env
                .get("USER")
                .context("username for db should be specified in env")?
                .to_string(),
            password: env
                .get("PASSWORD")
                .context("password for db should be specified in env")?
                .to_string(),
            collection,
        })
    }

    fn connect(&self) -> Result<Client> {
        let sep = if self.url.contains('?') { '&' } else { '?' };
        let connection = format!(
            "{}{}user={}&password={}",
            self.url, sep, self.user, self.password
        );
        Ok(Client::connect(&connection, NoTls)?)
    }

    pub fn download_collection(&self) -> Result<Vec<Organization>> {
        let mut client = self.connect()?;
        let rows = client.query(
            "select id, name, x, y, creation_date, turnover, full_name, employees_count, type, street, zip from organizations",
            &[],
        )?;
        rows.into_iter().map(row_to_org).collect()
    }

    pub fn register(&self, user_name: &str, user_hash: &str) -> Result<String> {
        let mut client = self.connect()?;
        match client.execute(
            "insert into users (username, password_hash) values ($1, $2)",
            &[&user_name, &user_hash],
        ) {
            Ok(_) => Ok("Пользователь успешно зарегистрирован".to_string()),
            Err(err) if err.code().map(|code| code.code()) == Some("23505") => {
                Err(anyhow!("Пользователь с таким именем уже зарегистрирован"))
            }
            Err(err) => Err(err.into()),
        }
    }

    pub fn login(&self, user_name: &str, user_hash: &str) -> Result<()> {
        let mut client = self.connect()?;
        let row = client
            .query_opt(
                "select password_hash from users where username = $1",
                &[&user_name],
            )?
            .ok_or_else(|| anyhow!("Пользователь с таким именем не найден"))?;
        let db_hash: String = row.get("password_hash");
        if db_hash == user_hash {
            Ok(())
        } else {
            Err(anyhow!("Неверный пароль"))
        }
    }

    pub fn create_session(&self, token: &str, username: &str) -> Result<()> {
        let mut client = self.connect()?;
        let expires_at = Utc::now().naive_utc() + Duration::minutes(15);
        client.execute(
            "insert into sessions (token, username, expires_at) values ($1, $2, $3)",
            &[&token, &username, &expires_at],
        )?;
        Ok(())
    }

    pub fn validate_token(&self, token: &str) -> Result<Option<String>> {
        let mut client = self.connect()?;
        let expires_at = Utc::now().naive_utc() + Duration::minutes(15);
        let row = client.query_opt(
            "update sessions set expires_at = $1 where token = $2 and expires_at > now() returning username",
            &[&expires_at, &token],
        )?;
        Ok(row.map(|row| row.get("username")))
    }

    pub fn add(&self, data: OrganizationData, user_name: &str) -> Result<()> {
        let mut client = self.connect()?;
        let employees_count = data.employees_count.map(i32::try_from).transpose()?;
        let row = client.query_one(
            "insert into organizations (name, x, y, creation_date, turnover, full_name, employees_count, type, street, zip, user_id)
             values ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, (select id from users where username = $11)) returning id",
            &[&data.name, &data.coordinates.x, &data.coordinates.y, &data.creation_date, &data.annual_turnover, &data.full_name,
              &employees_count, &data.organization_type.to_string(), &data.official_address.street, &data.official_address.zip_code, &user_name],
        )?;
        let id: i32 = row.get("id");
        self.collection
            .write()
            .unwrap()
            .add(Organization::new(id, data)?)?;
        Ok(())
    }

    fn check_permissions(&self, id: i32, user_name: &str) -> Result<bool> {
        let mut client = self.connect()?;
        let row = client.query_one(
            "select exists (select 1 from organizations o join users u on o.user_id = u.id where o.id = $1 and u.username = $2)",
            &[&id, &user_name],
        )?;
        Ok(row.get(0))
    }

    pub fn remove_by_id(&self, id: i32, user_name: &str) -> Result<()> {
        if !self.check_permissions(id, user_name)? {
            return Err(anyhow!("Доступ отказан из-за недостающих прав."));
        }
        let mut client = self.connect()?;
        client.execute("delete from organizations where id = $1", &[&id])?;
        self.collection.write().unwrap().remove(id);
        Ok(())
    }

    pub fn update_by_id(&self, id: i32, data: OrganizationData, user_name: &str) -> Result<()> {
        if !self.check_permissions(id, user_name)? {
            return Err(anyhow!("Доступ отказан из-за недостающих прав."));
        }
        let mut client = self.connect()?;
        let employees_count = data.employees_count.map(i32::try_from).transpose()?;
        client.execute(
            "update organizations set name = $1, x = $2, y = $3, creation_date = $4, turnover = $5, full_name = $6,
             employees_count = $7, type = $8, street = $9, zip = $10 where id = $11",
            &[&data.name, &data.coordinates.x, &data.coordinates.y, &data.creation_date, &data.annual_turnover, &data.full_name,
              &employees_count, &data.organization_type.to_string(), &data.official_address.street, &data.official_address.zip_code, &id],
        )?;
        self.collection.write().unwrap().update(id, data);
        Ok(())
    }

    pub fn remove_by_name_cmp(
        &self,
        data: &OrganizationData,
        user_name: &str,
        ord: Ordering,
    ) -> Result<usize> {
        let sql = match ord {
            Ordering::Greater => "delete from organizations where name > $1 and user_id = (select id from users where username = $2) returning id",
            Ordering::Less => "delete from organizations where name < $1 and user_id = (select id from users where username = $2) returning id",
            Ordering::Equal => return Ok(0),
        };
        let mut client = self.connect()?;
        let ids: Vec<i32> = client
            .query(sql, &[&data.name, &user_name])?
            .into_iter()
            .map(|row| row.get("id"))
            .collect();
        let mut collection = self.collection.write().unwrap();
        for id in &ids {
            collection.remove(*id);
        }
        Ok(ids.len())
    }
}

fn row_to_org(row: Row) -> Result<Organization> {
    let type_name: String = row.get("type");
    let employees_count: Option<i32> = row.get("employees_count");
    Organization::new(
        row.get("id"),
        OrganizationData {
            name: row.get("name"),
            coordinates: Coordinates::new(row.get("x"), row.get("y"))?,
            creation_date: row.get("creation_date"),
            annual_turnover: row.get("turnover"),
            full_name: row.get("full_name"),
            employees_count: employees_count.map(i64::from),
            organization_type: OrganizationType::parse(&type_name)?,
            official_address: Address {
                street: row.get("street"),
                zip_code: row.get("zip"),
            },
        },
    )
}
