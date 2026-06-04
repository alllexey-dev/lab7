package commands

import Response
import ServerContainer
import application.buildOrganization
import data.OrganizationTransferData
import data.Result
import domain.Organization
import java.sql.SQLException

class Update: Command {
    override val name = "update"
    override val args = listOf(
        "Id",
        "Name",
        "X",
        "Y",
        "Annual turnover",
        "Full name (unique)",
        "Employee count",
        "Street",
        "Zip code",
        "Type"
    )
    override val description = "Обновляет элемент в коллекции по заданному id"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Result {
        val dbManager = context.dBManager
        val id: Int
        return try {

            id = args[0].toInt()
            val preparedArgs = args.drop(1)
            val org: OrganizationTransferData = buildOrganization(preparedArgs)

            dbManager.updateById(id, org, userHash)

            Result(true, "Организация успешно обновлена.")
        } catch (e: NumberFormatException) {
            Result(false, e.message ?: "")
        } catch (_: SQLException) {
            Result(false, "Ошибка при работе с базой данных.")
        } catch (e: IllegalStateException) {Result(false, e.message ?: "")}

    }
}