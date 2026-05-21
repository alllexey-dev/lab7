package commands

import Response
import ServerContainer

class RemoveByID : Command {
    override val name = "remove_by_id"
    override val args = listOf("ID")
    override val description = "Удаляет из коллекции элемент по ID"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Response {
        val dbManager = context.dBManager
        try {
            dbManager.removeById(args[0].toInt(), userHash)
            return Response.Info("Элемент с ID ${args[0]} удален.")
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("Введенный аргумент не является числом.")
        } catch (e: IllegalStateException) {
            return Response.Error(e.message ?: "")
        }
    }
}