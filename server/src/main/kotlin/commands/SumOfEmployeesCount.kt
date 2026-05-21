package commands

import Response
import ServerContainer

class SumOfEmployeesCount : Command {
    override val name = "sum_of_employees_count"
    override val args = listOf<String>()
    override val description = "Возвращает количество работяг во всей коллекции"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val collectionManager = context.collectionManager
        val count = collectionManager.sumEmployees()

        return Response.Info("Общее количество работяг в коллекции: $count")
    }
}