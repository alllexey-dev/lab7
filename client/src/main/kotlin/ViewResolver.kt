class ViewResolver(
    private val context: ClientContainer,
) {
    fun resolve(response: Response){
        return when(response){
            is Response.Info -> {
                context.IO.printLine(response.message)
            }

            is Response.ResetTokenPlease ->{
                context.up()
            }

            is Response.Error -> {
                context.IO.printLine("ошибка: " + response.message)
            }

            is Response.Shutdown -> throw ExitSignal()

            is Response.HandShake -> {
                context.invoker.load(response)
                context.userToken = response.token
            }
        }
    }
}