package commands

import Response
import ServerContainer

class Show : Command {
    override val description: String = "Выводит список всех организаций"
    override val args = listOf<String>()
    override val name: String = "show"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val collectionManager = context.collectionManager
        if (collectionManager.getCollection()
                .isEmpty()
        ) return Response.Info("Вы еще не успели насоздавать шедевров...")
        val strBuilder = StringBuilder()
        collectionManager.getCollection().forEach { strBuilder.append(it) }
        return Response.Info(strBuilder.toString())
    }
}
