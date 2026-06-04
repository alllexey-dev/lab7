import java.sql.SQLException

class Dispatcher(
    val container: ServerContainer,
) {
    val invoker = container.commandInvoker
    val td = TokenDecoder()

    fun handleRequest(request: Request): Response {
        when (request) {
            is Request.ExecuteCommand -> try {
                val user = td.matchToken(request.userToken)
                val result = invoker.handleInput(request, user)
                return if (result.success) Response.Info(result.info)
                else Response.Error(result.info)
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
                val dbManager = container.dBManager

                val (passwordHashed, name) = request.userHash.split(" ")
                val user = Pair(passwordHashed, name)
                val token: String
                return if (request.enterType == EnterType.LOGIN && dbManager.login(name, passwordHashed).success) {
                    token = td.updateToken(user)

                    Response.HandShake(invoker.getCommands(), token)

                } else if (request.enterType == EnterType.REGISTER && dbManager.register(name, passwordHashed).success) {
                    token = td.updateToken(user)
                    Response.HandShake(invoker.getCommands(), token)

                } else Response.Error("Данное имя занято или введен неверный пароль.")


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