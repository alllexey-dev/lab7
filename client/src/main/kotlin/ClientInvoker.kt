import command.CommandSyntax
import commands.Command
import commands.ExecuteScript
import commands.Exit
import commands.Help

class ClientInvoker(
    private val context: ClientContainer,
) {
    val serverCommands = HashMap<String, CommandSyntax>()
    val clientCommands = HashMap<String, Command>()

    init {
        clientCommands["help"] = Help()
        clientCommands["exit"] = Exit()
        clientCommands["execute_script"] = ExecuteScript()
    }

    fun resolveCommand(command: String): Request? {
        clientCommands[command]?.let {
            if (command == "execute_script") {
                val name = context.IO.readLine()
                val scriptManager = context.scriptManager

                scriptManager.add(name ?: error("expected name"))

                if (!scriptManager.isRunning) {
                    resolveScript()
                }
            }
            clientCommands[command]!!.execute(context)
            return null
        }
        serverCommands[command]?.let {
            val syntax = getSyntax(command)
            val args = mutableListOf<String>()
            for (string in syntax) {
                context.IO.printBefore("$string: ")
                val arg = context.IO.readLine()
                if (arg != null) {
                    args.add(arg)
                }
            }
            return Request.ExecuteCommand(context.userToken,command, args)
        }

        throw IllegalArgumentException("Неизвестная комманда: $command")
    }

    private fun resolveScript() {
        val scriptManager = context.scriptManager
        scriptManager.isRunning = true
        while (scriptManager.buffer.isNotEmpty()) {
            val command = scriptManager.getLine()
            if (command == "execute_script") {
                val name = scriptManager.getLine()
                scriptManager.add(name)
            } else {
                val syntax = getSyntax(command)
                val args = mutableListOf<String>()
                for (i in syntax) {
                    val arg = scriptManager.getLine()
                    if (arg != "") {
                        args.add(arg)
                    }
                }
                val req = Request.ExecuteCommand(context.userToken,command, args)
                context.channelIO.write(req)

                val responseFromJson = context.channelIO.read() ?: return
                if (responseFromJson is Response.Error) {
                    scriptManager.panic()
                    throw ScriptError(responseFromJson.message)
                }
                context.resolver.resolve(responseFromJson)
            }
        }
        scriptManager.isRunning = false

    }

    fun getSyntax(name: String): List<String> {
        val command = serverCommands[name] ?: throw IllegalArgumentException("Неизвестная комманда: $name")
        return command.args
    }

    fun load(source: Response.HandShake) {
        val commandsSyntax = source.commands
        serverCommands.clear()
        commandsSyntax.forEach { serverCommands[it.name] = it }
    }

}