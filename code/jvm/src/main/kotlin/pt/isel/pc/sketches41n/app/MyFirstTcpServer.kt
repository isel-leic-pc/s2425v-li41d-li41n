package pt.isel.pc.sketches41n.app

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

class MyFirstTcpServer(
    private val address: InetSocketAddress,
) {
    fun run() {
        logger.info("Server is starting")
        val threadBuilder: Thread.Builder = Thread.ofPlatform()
        ServerSocket().use { serverSocket ->
            serverSocket.bind(address)
            logger.info("Server is listening in {}:{}", address.hostName, address.port)
            var nextClientId = 1
            while (true) {
                val clientSocket = serverSocket.accept()
                val clientId = nextClientId++
                threadBuilder.start {
                    handleClientLoop(clientSocket, clientId)
                }
            }
        }
        logger.info("Server is stopping")
    }

    private fun handleClientLoop(
        clientSocket: Socket,
        clientId: Int,
    ) {
        logger.info(
            "{}: Starting handling client {}",
            clientId,
            getRemoteAddress(clientSocket),
        )
        clientSocket.inputStream.bufferedReader().use { reader ->
            clientSocket.outputStream.bufferedWriter().use { writer ->
                writer.writeLine("Welcome, you are client $clientId")
                var nextMessageId = 0
                while (true) {
                    val line = reader.readLine()
                    logger.info("{}: Read line: {}", clientId, line)
                    if (line == null) {
                        logger.info("{}: Ending handling of client {}", clientId)
                        break
                    }
                    writer.writeLine("${nextMessageId++}: ${line.uppercase()}")
                }
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MyFirstTcpServer::class.java)

        @JvmStatic
        fun main(args: Array<String>) {
            val server = MyFirstTcpServer(InetSocketAddress("0.0.0.0", 8080))
            server.run()
        }

        fun getRemoteAddress(socket: Socket): String {
            val inetSocketAddress =
                socket.remoteSocketAddress as? InetSocketAddress
                    ?: return "<unknown>"
            return inetSocketAddress.hostName
        }
    }
}
