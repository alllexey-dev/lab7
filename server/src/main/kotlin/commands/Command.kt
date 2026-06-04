package commands

import data.Result
import ServerContainer
import command.CommandSyntax

interface Command {
    val name: String
    val args: List<String>
    val description: String
    fun execute(context: ServerContainer, args: List<String>, userHash: String): Result
    fun getSyntax(): CommandSyntax {
        return CommandSyntax(name, args, description)
    }
}
