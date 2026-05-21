package commands

import Response
import ServerContainer
import application.buildOrganization
import domain.Organization

class RemoveLower : Command {
    override val name = "remove_lower"
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
    override val description = "Удаляет из коллекции все элементы, меньше чем"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val dbManager = context.dBManager
        val collectionManager = context.collectionManager

        val org: Organization = buildOrganization(args)
        val count = collectionManager.countLower(org)

        dbManager.removeLower(org, userHash)
        return Response.Info("Из коллекции удалено $count элементов")
    }
}