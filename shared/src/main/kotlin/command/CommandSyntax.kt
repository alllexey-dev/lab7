package command
import kotlinx.serialization.Serializable

@Serializable
data class CommandSyntax(
    val name: String,
    val args: List<String>,
    val description: String,
)