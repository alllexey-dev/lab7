import java.net.InetSocketAddress
import java.nio.channels.SocketChannel

class Balancer {

    var availableServers: ArrayList<SocketChannel> = ArrayList()
    var counter = 0

    fun handle(request: Request): Response {
        when (request) {
            is Request.HiBalancer -> {
                registerServer(request.host, request.port)
                return Response.Pong
            }

            else -> return roundRobin(request)
        }
    }

    private fun registerServer(host: String, port: Int) {
        availableServers.add(
            SocketChannel.open(
                InetSocketAddress(
                    host,
                    port,
                )
            )
        )
    }

    fun roundRobin(request: Request): Response {
        if (request is Request.HiBalancer) {
            registerServer(request.host, request.port)
            return Response.Pong
        }

        val channel = try {
            getNextServer()
        } catch (_: IllegalStateException) {
            return Response.Error("нет вставших серверов(")
        }

        channel.read()

        return channel.read()!!
    }

    private fun getNextServer(): GatewayToServersChannel {
        if (availableServers.isEmpty()) {
            throw IllegalStateException()
        }

        val index = counter % availableServers.size

        val candidate = availableServers[index]

        val channel = GatewayToServersChannel(candidate)

        channel.write(Request.Ping)

        if (channel.read() !is Response.Pong) {
            availableServers.drop(counter)
            return getNextServer()
        }

        counter++

        return channel
    }
}