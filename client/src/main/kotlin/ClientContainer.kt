import util.PropertiesParser
import java.io.IOException
import java.net.ConnectException
import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

open class ClientContainer {
    val resolver = ViewResolver(this)
    val IO: IOPort = CliManager()
    val clientEnt = Client(this)
    var socket: SocketChannel? = null
    val scriptManager = ScriptManager()
    val invoker: ClientInvoker = ClientInvoker(this)
    lateinit var channelIO: ChannelIO
    var timeout: Long = 5000
    var serverPort = ""
    var hostname = ""
    init {
        val env = PropertiesParser.getPropertiesFromFile(".env")
        serverPort = env["SERVER_PORT"] ?: throw Error("server port should be specified in env")
        hostname = env["HOST_NAME"] ?: throw Error("hostname should be specified in env")
    }

    fun up() {
        val address = InetSocketAddress(
            hostname,
            serverPort.toIntOrNull() ?: throw Error("check server port format in env file")
        )
        try {
            val client = SocketChannel.open(address)
            client.configureBlocking(true)
            socket = client
            channelIO = ChannelIO(client)
            channelIO.write(Request.HandShake(null))
            val handshakeResponse = channelIO.read() ?: return up()
            resolver.resolve(handshakeResponse)
            timeout = 5000
            while (true) {
                clientEnt.run()
            }
        } catch (_: ExitSignal) {
            return
        } catch (_: IllegalStateException) {
            return
        } catch (_: ConnectException) {
            IO.printLine("cannot connect to server")
            Thread.sleep(timeout)
            if (timeout < 50000) timeout += 1000
            return up()
        } catch (_: IOException) {
            IO.printLine("сервер разорвал подключение")
            return up()
        }
    }
}

fun start(init: ClientContainer.() -> Unit): ClientContainer{
    val container = ClientContainer()
    container.init()
    return container
}