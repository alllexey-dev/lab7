mod app;
mod collection;
mod db;

use anyhow::{anyhow, Context, Result};
use app::App;
use collection::CollectionManager;
use db::Database;
use lab7_shared::{read_env, read_frame, write_frame, Request, Response};
use std::net::{TcpListener, TcpStream};
use std::sync::{Arc, RwLock};
use std::thread;

fn handle_client(app: Arc<App>, mut stream: TcpStream) -> Result<()> {
    let request: Request = read_frame(&mut stream)?;
    println!("SERVER GOT REQUEST: {request:?}");
    let response = app.handle(request);
    println!("SERVER RESPONSE: {response:?}");
    write_frame(&mut stream, &response)
}

fn main() -> Result<()> {
    let env = read_env(".env")?;
    let host = env
        .get("HOST_NAME")
        .context("hostname should be specified in env")?
        .clone();
    let port: u16 = env
        .get("SERVER_PORT")
        .context("server port should be specified in env")?
        .parse()?;
    let gw_host = env
        .get("GW_HOST")
        .context("GW_HOST should be specified in env")?
        .clone();
    let gw_port: u16 = env
        .get("GW_PORT")
        .context("GW_PORT should be specified in env")?
        .parse()?;

    let collection = Arc::new(RwLock::new(CollectionManager::default()));
    let db = Database::new(&env, collection.clone())?;
    collection
        .write()
        .map_err(|_| anyhow!("Collection lock is poisoned"))?
        .upload(db.download_collection()?);
    let app = Arc::new(App { db, collection });

    if let Ok(mut gateway) = TcpStream::connect((gw_host.as_str(), gw_port)) {
        let _ = write_frame(
            &mut gateway,
            &Request::HiBalancer {
                host: host.clone(),
                port,
            },
        );
        let _: Result<Response> = read_frame(&mut gateway);
    }

    let listener = TcpListener::bind((host.as_str(), port))?;
    println!("Server started at {host}:{port}");
    for stream in listener.incoming() {
        let app = app.clone();
        match stream {
            Ok(stream) => {
                thread::spawn(move || {
                    if let Err(err) = handle_client(app, stream) {
                        eprintln!("{err:?}");
                    }
                });
            }
            Err(err) => eprintln!("{err:?}"),
        }
    }
    Ok(())
}
