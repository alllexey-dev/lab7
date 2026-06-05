import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.logger
import util.PropertiesParser
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import kotlin.text.isBlank
import kotlin.text.split

open class GatewayContainer {
    val balancer = Balancer()
    var logger = logger()
    var serverPort = ""
    var hostname = ""
    init {
        val env = PropertiesParser.getPropertiesFromFile(".env")
        serverPort = env["GW_PORT"] ?: throw Error("server port should be specified in env")
        hostname = env["GW_HOST"] ?: throw Error("hostname should be specified in env")
    }

    fun up() {
        try {
            val selector = Selector.open()
            val serverSocket = ServerSocketChannel.open()
            serverSocket.bind(
                InetSocketAddress(
                    hostname,
                    serverPort.toIntOrNull() ?: throw Error("check for server port format in env file")
                )
            )
            serverSocket.configureBlocking(false)
            serverSocket.register(selector, SelectionKey.OP_ACCEPT)

            println("Gateway started at 127.0.0.1:$serverPort")
            while (true) {
                processInput()
                selector.selectNow()
                val selectionIterator = selector.selectedKeys().iterator()
                while (selectionIterator.hasNext()) {
                    val key = selectionIterator.next()
                    logger.info { key.toString() }
                    selectionIterator.remove()

                    if (!key.isValid) continue

                    if (key.isAcceptable) {
                        val client = serverSocket.accept()
                        logger.info { client.toString() }
                        client.configureBlocking(false)

                        val io = ClientsToGatewayChannel(client)
                        client.register(selector, SelectionKey.OP_READ, io)

                        println("Client connected: ${client.remoteAddress}")
                    }

                    if (key.isReadable) {

                        val io = key.attachment() as ClientsToGatewayChannel

                        try {

                            val request = io.read()
                            logger.info { request.toString() }
                            request?.let {
                                println("Получен запрос: $request")
                                val response = balancer.handle(request)
                                logger.info { response }
                                try {
                                    io.write(response)
                                } catch (e: Exception) {
                                    logger.warn { e.message ?: "" }
                                    e.printStackTrace()
                                }
                            }
                        } catch (e: Exception) {
                            logger.info { e.message }
                            println("Клиент отключился или произошла ошибка")
                            println(e.printStackTrace())
                            key.channel().close()
                            key.cancel()
                        }
                    }
                }
            }
        } catch (_: ExitSignal) {
            println("Сервер выключается.")
            return
        }
    }
    fun processInput(){
        val input: String?
        input = if (System.`in`.available() > 0) {
            readlnOrNull()
        } else null

        if (input != null) {
            try {
                if (!input.isBlank()) {
                    val tokens = input.split(" ")
                    val name = tokens[0]
                    val args = tokens.drop(1)
                    logger.log(Level.INFO, "$name, $args")
                    if (input.equals("shutdown")) throw ExitSignal()

                }
            } catch (e: IllegalArgumentException) {
                println(e.message ?: "")
            }
        }
    }
}
