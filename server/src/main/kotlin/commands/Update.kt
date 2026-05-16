package commands

import Response
import ServerContainer
import application.buildOrganization
import domain.Organization

class Update: Command {
    override val name = "update"
    override val args = listOf(
        "ID",
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

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val dbManager = context.dBManager

        val id: Int
        try {
            id = args[0].toInt()
        } catch (_: Throwable) {
            return Response.Error("Неверный формат аргумента.")
        }
        val preparedArgs = args.drop(1)
        val org: Organization = buildOrganization(preparedArgs)
        try {
            dbManager.updateById(id, org, userHash)
        }  catch (e: IllegalStateException){
            return Response.Error(e.message ?: "")
        }
        return Response.Info("Организация успешно обновлена.")

    }
}