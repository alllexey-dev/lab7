import kotlinx.serialization.Serializable

@Serializable
sealed class Request {
    @Serializable
    data class HandShake(
        val userHash: String
    ) : Request()

    @Serializable
    data class ExecuteCommand(
        val userToken: String,
        val commandName: String,
        val args: List<String>,
    ) : Request()

    @Serializable
    object Ping : Request()

    @Serializable
    data class HiBalancer(
        val host: String,
        val port: Int,
    ) : Request()
}