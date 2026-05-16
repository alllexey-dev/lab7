import java.sql.SQLException

class Dispatcher(
    container: ServerContainer,
) {
    val invoker = container.commandInvoker
    val td = TokenDecoder()

    fun handleRequest(request: Request): Response {
        when (request) {
            is Request.ExecuteCommand -> try {
                val skip = td.matchToken(request.userToken)
                val result = invoker.handleInput(request)
                return result
            } catch (_: TokenExpiredException) {
                return Response.ResetTokenPlease
            } catch (_: ExitSignal) {
                return Response.Shutdown
            } catch (_: SQLException){
                println("не удалось взаимодействовать с базой данных")
            }

            catch (e: Exception) {
                val rpc = Response.Error(e.message ?: "No error message specified")

                return rpc
            }

            is Request.HandShake -> try {
                val token = td.updateToken(request.userHash)
                val response = Response.HandShake(invoker.getCommands(), token)
                return response
            } catch (e: Exception) {
                println(e.message ?: "No error message specified")
            }

            is Request.Ping -> {
                return Response.Pong
            }

            else -> {}
        }

        return Response.Error("Something went wrong")
    }
}