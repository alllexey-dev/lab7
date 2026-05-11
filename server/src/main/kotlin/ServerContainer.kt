import application.CommandInvoker
import data.StorageManager
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.logger
import util.PropertiesParser
import java.net.InetSocketAddress
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel

class ServerContainer(
    filePath: String,
) {
    val commandInvoker = CommandInvoker(this)
    val dispatcher: Dispatcher = Dispatcher(this)
    val storageManager: StorageManager = StorageManager(this, filePath)
    val collectionManager = application.CollectionManager(storageManager.downloadCollection())
    val IO = ServerCli(this)
    val logger = logger()
    var serverPort = ""
    var hostname = ""

    init {
        val env = PropertiesParser.getPropertiesFromFile(".env")
        serverPort = env["SERVER_PORT"] ?: throw Error("server port should be specified in env")
        hostname = env["HOST_NAME"] ?: throw Error("hostname should be specified in env")
    }

    fun up() {
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
//                        println(Request?.data)
                        if (request != null) {
                            println("Получен запрос: $request")
                            val response = dispatcher.handleRequest(request)
                            logger.info { response }
//                            println(Response.data)
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
                        //println(e.printStackTrace())
                        key.channel().close()
                        key.cancel()
                    }
                }
            }
        }
    }
}