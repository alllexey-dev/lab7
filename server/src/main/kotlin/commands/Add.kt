package commands

import Response
import ServerContainer
import application.buildOrganization
import domain.Organization
import data.OrganizationTransferData
import data.Result
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

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Result {
        val dbManager = context.dBManager
        return try {
            val org: OrganizationTransferData = buildOrganization(args)
            dbManager.add(org, userHash)
            Result(true, "Организация успешно добавлена")
        } catch (e: IllegalArgumentException) {
            Result(false, e.message ?: "No message specified")
        }
    }
}
