import command.CommandSyntax
import kotlinx.serialization.Serializable

@Serializable
sealed class Response {
    @Serializable
    data class HandShake(
        val commands: List<CommandSyntax>,
        val token: String,
    ) : Response()

    @Serializable
    data class Info(
        val message: String,
    ) : Response()

    @Serializable
    data class Error(
        val message: String,
    ) : Response()

    @Serializable
    object ResetTokenPlease: Response()

    @Serializable
    object Shutdown : Response()
}