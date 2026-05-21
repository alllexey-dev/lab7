package commands.inner

import ServerContainer
import java.io.FileNotFoundException

class Save: InnerCommand {
    override val name = "save"
    override val description = "Сохраняет текущую коллекцию"
    override fun execute(context: ServerContainer, args: List<String>) {
        var path = ""
        if (args.isNotEmpty()){
            path = args[0]
        }
        try {

        } catch (_: FileNotFoundException) {
            throw IllegalArgumentException("Указан неверный путь к файлу.")
        }
    }
}