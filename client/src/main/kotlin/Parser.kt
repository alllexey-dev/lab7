class Parser(val container: ClientContainer) {
    val io = container.IO

    fun parse(): Request{
        val mainLine = io.readLine()

        val tokens = mainLine?.split(" ") ?: throw IllegalArgumentException()


        return Request.ExecuteCommand(
            container.userToken,
            tokens[0],
            tokens.drop(1)
        )

    }
}