package data

import application.CollectionManager
import domain.Address
import domain.Coordinates
import domain.Organization
import domain.OrganizationType
import util.PropertiesParser
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.time.LocalDate

class DBManager(val collectionManager: CollectionManager) {
    var connection: Connection
    val url: String
    val user: String
    val password: String
    var initDate: LocalDate

    init {
        val env = PropertiesParser.getPropertiesFromFile(".env")
        url = env["URL"] ?: throw Error("url for db should be specified in env")
        user = env["USER"] ?: throw Error("username for db should be specified in env")
        password = env["PASSWORD"] ?: throw Error("password for db should be specified in env")
        connection = DriverManager.getConnection(url, user, password)
        initDate = LocalDate.now()
    }


    fun clear() {
        val statement = "delete from organizations"

        connection.prepareStatement(statement).use { sqlStatement ->
            sqlStatement.executeQuery()
        }

        collectionManager.clear()
    }

    fun removeGreater(organization: Organization) {
        val statement = "delete from organizations where name > ?"

        connection.prepareStatement(statement).use { sqlStatement ->
            sqlStatement.setString(1, organization.name)
            sqlStatement.executeUpdate()
        }

        collectionManager.removeGreater(organization)
    }

    fun removeLower(organization: Organization) {
        val statement = "delete from organizations where name < ?"

        connection.prepareStatement(statement).use { sqlStatement ->
            sqlStatement.setString(1, organization.name)
            sqlStatement.executeUpdate()
        }

        collectionManager.removeLower(organization)
    }

    fun removeById(id: Int) {


        val statement = "delete from organizations where id = ?"

        connection.prepareStatement(statement).use { sqlStatement ->
            sqlStatement.setInt(1, id)
            sqlStatement.executeUpdate()
        }

        collectionManager.removeById(id)
    }

    fun updateById(id: Int, organization: Organization) {
        val statement = "update organizations " +
                "set name = ?, x = ?, y = ?, creation_date = ?, " +
                "turnover = ?, full_name = ?, employees_count = ?, " +
                "type = ?, street = ?, zip = ? " +
                "where id = ?"

        connection.prepareStatement(statement).use { sqlStatement ->
            sqlStatement.setString(1, organization.name)
            sqlStatement.setFloat(2, organization.coordinates.x)
            sqlStatement.setFloat(3, organization.coordinates.y)
            sqlStatement.setObject(4, organization.creationDate)
            sqlStatement.setFloat(5, organization.annualTurnover)
            sqlStatement.setString(6, organization.fullName)
            sqlStatement.setInt(7, (organization.employeesCount ?: 0).toInt())
            sqlStatement.setString(8, organization.type.toString())
            sqlStatement.setString(9, organization.officialAddress.street)
            sqlStatement.setString(10, organization.officialAddress.zipCode)
            sqlStatement.setInt(11, id)

            sqlStatement.executeUpdate()
        }

        collectionManager.updateById(id, organization)
    }

    fun add(organization: Organization) {
        val statement =
            "insert into organizations (name, x, y, creation_date, turnover, full_name, employees_count, type, street, zip)" +
                    "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"

        connection.prepareStatement(statement).use { sqlStatement ->
            sqlStatement.setString(1, organization.name)
            sqlStatement.setFloat(2, organization.coordinates.x)
            sqlStatement.setFloat(3, organization.coordinates.y)
            sqlStatement.setString(4, organization.creationDate.toString())
            sqlStatement.setFloat(5, organization.annualTurnover)
            sqlStatement.setString(6, organization.fullName)
            sqlStatement.setInt(7, (organization.employeesCount ?: 0).toInt())
            sqlStatement.setString(8, organization.type.toString())
            sqlStatement.setString(9, organization.officialAddress.street)
            sqlStatement.setString(10, organization.officialAddress.zipCode)

            sqlStatement.executeUpdate()
        }

        val idStatement = "SELECT last_value FROM organizations_id_seq"

        val id = connection.prepareStatement(idStatement).executeQuery().use { resultSet ->
            if (resultSet.next()) {
                resultSet.getInt(1)
            } else {
                throw SQLException("Не удалось получить значение последовательности")
            }
        }

        collectionManager.add(organization, id)
    }

    fun downloadCollection(): List<Organization> {

        val organizationsList: ArrayList<Organization> = ArrayList()

        val query = """
        SELECT id, name, x, y, creation_date, turnover, full_name, employees_count, type, street, zip 
        FROM organizations
    """.trimIndent()
        connection.prepareStatement(query).use { stmt ->
            stmt.executeQuery().use { rs ->
                while (rs.next()) {

                    val org = Organization(
                        rs.getInt("id"),
                        rs.getString("name"),
                        Coordinates(
                            rs.getFloat("x"),
                            rs.getFloat("y"),
                        ),
                        LocalDate.parse(rs.getString("creation_date")),
                        rs.getFloat("turnover"),
                        rs.getString("full_name"),
                        rs.getInt("employees_count").toLong(),
                        OrganizationType.valueOf(rs.getString("type")),
                        Address(
                            rs.getString("street"),
                            rs.getString("zip"),
                        ),
                    )

                    organizationsList.add(org)
                }
            }
        }

        return organizationsList
    }
}