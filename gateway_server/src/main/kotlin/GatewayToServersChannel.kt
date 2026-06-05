import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.SocketChannel

class GatewayToServersChannel(
    private val channel: SocketChannel,
) {
    private var size = -1
    private val sizeBuffer = ByteBuffer.allocate(4)
    private lateinit var dataBuffer: ByteBuffer

    fun read(): Response? {
        if (size == -1) {
            val bytesRead = channel.read(sizeBuffer)
            if (bytesRead == -1) throw IOException("Соединение с сервером разорвано")
            if (sizeBuffer.hasRemaining()) return null

            sizeBuffer.flip()
            size = sizeBuffer.int
            sizeBuffer.clear()

            dataBuffer = ByteBuffer.allocate(size)
        }

        val bytesReadData = channel.read(dataBuffer)
        if (bytesReadData == -1) throw IOException("Соединение с сервером разорвано")

        if (dataBuffer.hasRemaining()) return null

        val json = String(dataBuffer.array(), Charsets.UTF_8)
        val rpc = Json.decodeFromString<Response>(json)

        size = -1

        return rpc
    }

    fun write(message: Request) {
        val json = Json.encodeToString<Request>(message)
        val bodyBytes = json.toByteArray(Charsets.UTF_8)

        val writeBuffer = ByteBuffer.allocate(4 + bodyBytes.size)
        writeBuffer.putInt(bodyBytes.size)
        writeBuffer.put(bodyBytes)
        writeBuffer.flip()

        while (writeBuffer.hasRemaining()) {
            val written = channel.write(writeBuffer)
            if (written == -1) throw Exception("Disconnected while writing")
        }
    }
}
