package commands

import Response
import ServerContainer

class Info(
) : Command {
    override val name = "info"
    override val args = listOf<String>()
    override val description = "Выводит информацию о коллекции"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val collectionManager = context.collectionManager
        val collection = collectionManager.getCollection()

        if (collection.isEmpty()) {
            return Response.Info("коллекция пуста:(")
        } else {
            val strBuilder = StringBuilder()
            strBuilder.append("Количество элементов в коллекции: ${collection.size}\n")
            strBuilder.append("дата создания шедевра - ${collectionManager.getInitializationDate()}\n")

            val collect = collectionManager.getCollection()

            strBuilder.append("Организации в коллекции:\n")

            collect.forEach { strBuilder.append("${it.fullName} с id номер ${it.id}\n") }
            return Response.Info(strBuilder.toString())
        }

    }
}