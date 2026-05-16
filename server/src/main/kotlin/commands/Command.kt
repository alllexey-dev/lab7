package commands

import Response
import ServerContainer
import command.CommandSyntax

interface Command {
    val name: String
    val args: List<String>
    val description: String
    fun execute(context: ServerContainer, args: List<String>, userHash: String): Response
    fun getSyntax(): CommandSyntax {
        return CommandSyntax(name, args, description)
    }
}
