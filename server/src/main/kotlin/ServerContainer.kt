import application.CommandInvoker
import data.DBManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.logger
import util.PropertiesParser
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel

class ServerContainer {
    var commandInvoker = CommandInvoker(this)
    val dispatcher: Dispatcher = Dispatcher(this)
    val collectionManager = application.CollectionManager()
    val dBManager = DBManager(collectionManager)
    val IO = ServerCli(this)
    val logger = logger()
    var serverPort = ""
    var hostname = ""

    init {
        val env = PropertiesParser.getPropertiesFromFile(".env")
        serverPort = env["SERVER_PORT"] ?: throw Error("server port should be specified in env")
        hostname = env["HOST_NAME"] ?: throw Error("hostname should be specified in env")
        collectionManager.uploadCollection(dBManager.downloadCollection())

        val balancerPort = env["GW_PORT"] ?: throw Error("hostname should be specified in env")
        val balancerHost = env["GW_HOST"] ?: throw Error("hostname should be specified in env")

        val address = InetSocketAddress(
            balancerHost,
            balancerPort.toIntOrNull() ?: error("no")
        )
        SocketChannel.open(address).use{ socketChannel ->
            val json = Json.encodeToString(Request.HiBalancer(
                hostname,
                serverPort.toIntOrNull() ?: throw Error("check for server port format in env file")
            ))
            val bodyBytes = json.toByteArray(Charsets.UTF_8)

            val writeBuffer = ByteBuffer.allocate(4 + bodyBytes.size)
            writeBuffer.putInt(bodyBytes.size)
            writeBuffer.put(bodyBytes)
            writeBuffer.flip()

            while (writeBuffer.hasRemaining()) {
                val written = socketChannel.write(writeBuffer)
                if (written == -1) throw Exception("Disconnected while writing")
            }

        }
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

            println("Server started at 127.0.0.1:$serverPort")
            while (true) {
                val input = IO.processInput()
                if (input != null) {
                    logger.info { input }
                    try {
                        if (!input.isBlank()) {
                            val tokens = input.split(" ")
                            val name = tokens[0]
                            val args = tokens.drop(1)
                            logger.log(Level.INFO, "$name, $args")
                            commandInvoker.invoke(name, args)

                        }
                    } catch (e: IllegalArgumentException) {
                        IO.write(e.message ?: "")
                        logger.warn { e.message ?: "" }
                    }
                }
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

                        val io = ServerChannelIO(client)
                        client.register(selector, SelectionKey.OP_READ, io)

                        println("Client connected: ${client.remoteAddress}")
                    }

                    if (key.isReadable) {

                        val io = key.attachment() as ServerChannelIO

                        try {

                            val request = io.read()
                            logger.info { request.toString() }
                            request?.let {
                                println("Получен запрос: $request")
                                val response = dispatcher.handleRequest(request)
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
}

fun serverContainer(container: ServerContainer.() -> Unit): ServerContainer {
    val serv = ServerContainer()
    serv.container()
    return serv
}