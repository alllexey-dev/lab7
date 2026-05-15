package commands.inner

import ServerContainer

class Shutdown (
): InnerCommand {
    override val name = "shutdown"
    override val description = "Завершает процесс сервера"

    override fun execute(context: ServerContainer, args: List<String>){
        throw ExitSignal()
    }
}