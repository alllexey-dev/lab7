package application

import Request
import Response
import ServerContainer
import command.CommandSyntax
import commands.*
import commands.Add
import commands.inner.InnerCommand
import commands.inner.Save
import commands.inner.Shutdown

class CommandInvoker(val context: ServerContainer) {
    private val commands = mutableMapOf<String, Command>()
    private val serverCommands = mutableMapOf<String, InnerCommand>()
    private val add = Add()
    private val show = Show()
    private val countByType = CountByType()
    private val info = Info()
    private val sumOfEmployeesCount = SumOfEmployeesCount()
    private val countLessThanOfficialAddress = CountLessThanOfficialAddress()
    private val removeLower = RemoveLower()
    private val removeGreater = RemoveGreater()
    private val removeByID = RemoveByID()
    private val update = Update()
    fun registerCommand(command: Command) {
        commands[command.name] = command
    }

    init {
        registerCommand(show)
        registerCommand(add)
        registerCommand(countByType)
        registerCommand(info)
        registerCommand(sumOfEmployeesCount)
        registerCommand(countLessThanOfficialAddress)
        registerCommand(removeLower)
        registerCommand(removeGreater)
        registerCommand(removeByID)
        registerCommand(update)
        serverCommands["shutdown"] = Shutdown()
        serverCommands["save"] = Save()
    }

    fun handleInput(req: Request.ExecuteCommand): Response {
        val commandName = req.commandName
        val command = commands[commandName] ?: return  Response.Error("команда не найдена")

        return command.execute(context, req.args)
    }

    fun invoke(name: String, args: List<String>){
        serverCommands[name]?.execute(context, args) ?: throw IllegalArgumentException("Команды $name не существует.")
    }

    fun getCommands(): List<CommandSyntax>{
        val commandSyntaxes = mutableListOf<CommandSyntax>()
        commands.forEach {commandSyntaxes.add(it.value.getSyntax())}
        return commandSyntaxes
    }

}