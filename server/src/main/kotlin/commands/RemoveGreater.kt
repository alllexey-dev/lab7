package commands

import Response
import ServerContainer
import application.buildOrganization
import domain.Organization

class RemoveGreater : Command {
    override val name = "remove_greater"
    override val args = listOf(
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
    override val description = "Удаляет из коллекции все элементы, превышающие заданный"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val dbManager = context.dBManager
        val collectionManager = context.collectionManager

        val org: Organization = buildOrganization(args)
        val count = collectionManager.countGreater(org)

        dbManager.removeGreater(org, userHash)
        return Response.Info("Из коллекции удалено $count элементов")
    }
}