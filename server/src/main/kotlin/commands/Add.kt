package commands

import Response
import ServerContainer
import application.buildOrganization
import domain.Organization

class Add : Command {
    override val description: String = "Добавляет организацию в коллекцию"
    override val args: List<String> = listOf(
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
    override val name: String = "add"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val dbManager = context.dBManager

        val org: Organization = buildOrganization(args)

        dbManager.add(org, userHash)
        return Response.Info("Организация '${org.name}' успешно добавлена.")
    }

}
