import application.CommandInvoker
import data.DBManager
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.kotlin.logger
import thread.RequestResolver
import util.PropertiesParser
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.SelectionKey
import java.nio.channels.Selector
import java.nio.channels.ServerSocketChannel
import java.nio.channels.SocketChannel
import java.util.concurrent.ForkJoinPool

class ServerContainer {
    var commandInvoker = CommandInvoker(this)
    val dispatcher: Dispatcher = Dispatcher(this)
    val collectionManager = application.CollectionManager()
    val dBManager = DBManager(collectionManager)
    val IO = ServerCli(this)
    val logger = logger()
    var serverPort = ""
    var hostname = ""

    val readPool = ForkJoinPool(4)
    val writePool = ForkJoinPool(4)
    val requestResolver = RequestResolver()

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

            println("Server started at $hostname:$serverPort")
            while (true) {
                process(selector, serverSocket)
            }
        } catch (_: ExitSignal) {
            requestResolver.shutdown()
            writePool.shutdown()
            readPool.shutdown()
            println("Сервер выключается.")
            return
        }
    }

    fun process(selector: Selector, serverSocket: ServerSocketChannel) {
        IO.process()
        selector.selectNow()
        val selectionIterator = selector.selectedKeys().iterator()
        while (selectionIterator.hasNext()) {
            val key = selectionIterator.next()
            logger.info { key.toString() }
            selectionIterator.remove()
            if (!key.isValid) continue

            if (key.isAcceptable) {
                val clientChannel = serverSocket.accept()
                logger.info { clientChannel.toString() }
                clientChannel.configureBlocking(false)

                val client = ClientState(clientChannel)
                clientChannel.register(selector, SelectionKey.OP_READ, client)

                println("Client connected: ${clientChannel.remoteAddress}")
            }

            if (key.isReadable) {
                val state = key.attachment() as ClientState

                var shouldRead = false

                state.lock.lock()
                try {
                    if (!state.isReading && !state.isClosed) {
                        state.isReading = true
                        shouldRead = true
                    }
                } finally {
                    state.lock.unlock()
                }

                if (shouldRead) {
                    readPool.execute {
                        try {
                            val request = state.read()
                            IO.write(request.toString() + " from: " + state.channel.remoteAddress)
                            if (request != null) {
                                requestResolver.execute {
                                    try {
                                        val response = dispatcher.handleRequest(request)

                                        logger.info { response }

                                        submitWrite(key, state, response)
                                    } catch (e: Exception) {
                                        logger.warn { e.message ?: "" }
                                        e.printStackTrace()
                                        closeKey(key, state)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn { e.message ?: "" }
                            closeKey(key, state)
                        } finally {
                            state.lock.lock()
                            try {
                                state.isReading = false
                            } finally {
                                state.lock.unlock()
                            }
                        }
                    }
                }
            }

        }
    }

    private fun submitWrite(
        key: SelectionKey,
        state: ClientState,
        response: Response
    ) {
        @Suppress
        var shouldWrite = false

        state.lock.lock()
        try {
            if (!state.isWriting && !state.isClosed) {
                state.isWriting = true
                shouldWrite = true
            }
        } finally {
            state.lock.unlock()
        }

        if (!shouldWrite) {
            return
        }

        writePool.execute {
            try {
                state.write(response)
            } catch (e: Exception) {
                logger.warn { e.message ?: "" }
                e.printStackTrace()
                closeKey(key, state)
            } finally {
                state.lock.lock()
                try {
                    state.isWriting = false
                } finally {
                    state.lock.unlock()
                }
            }
        }
    }
}

private fun closeKey(key: SelectionKey, state: ClientState) {
    state.lock.lock()
    try {
        if (state.isClosed) return
        state.isClosed = true
    } finally {
        state.lock.unlock()
    }

    try {
        key.cancel()
    } catch (_: Exception) {
    }

    try {
        key.channel().close()
    } catch (_: Exception) {
    }
}

fun serverContainer(container: ServerContainer.() -> Unit): ServerContainer {
    val serv = ServerContainer()
    serv.container()
    return serv
}