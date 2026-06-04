package commands

import Response
import ServerContainer
import data.Result

class RemoveByID : Command {
    override val name = "remove_by_id"
    override val args = listOf("Id")
    override val description = "Удаляет из коллекции элемент по Id"

    override fun execute(context: ServerContainer, args: List<String>, userHash: String): Result {
        val dbManager = context.dBManager
        return try {
            dbManager.removeById(args[0].toInt(), userHash)
            Result(true, "Элемент с Id ${args[0]} удален.")
        } catch (_: NumberFormatException) {
            Result(false, "Введенный аргумент не является числом.")
        } catch (e: IllegalStateException) {
            Result(false, e.message ?: "")
        }
    }
}