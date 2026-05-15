import org.apache.logging.log4j.Level

class ServerCli(
    val context: ServerContainer
) {
    fun processInput(): String?{
        if (System.`in`.available() > 0) {
            val input = readlnOrNull()
            return input
        }
        return null
    }
    fun write(message: String){
        println(message)
    }

    fun process(){
        val commandInvoker = context.commandInvoker
        val logger = context.logger
        val input = processInput()
        if (input != null) {
            logger.info { input }
            try {
                if (!input.isBlank()) {
                    val tokens = input.split(" ")
                    val name = tokens[0]
                    val args = tokens.drop(1)
                    logger.log(Level.INFO, "$name, $args")
                    commandInvoker.invoke(name, args)

                }
            } catch (e: IllegalArgumentException) {
                write(e.message ?: "")
                logger.warn { e.message ?: "" }
            }
        }
    }
}