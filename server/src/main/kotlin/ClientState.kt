import java.nio.channels.SocketChannel
import java.util.concurrent.locks.ReentrantLock

class ClientState(val channel: SocketChannel) {
    private val io = ServerChannelIO(channel)
    var isReading = false
    var isWriting = false
    var isClosed = false
    val lock = ReentrantLock()
    fun read() = io.read()
    fun write(response: Response) = io.write(response)
}