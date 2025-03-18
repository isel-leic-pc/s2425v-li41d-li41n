package pt.isel.pc.sketches.apps

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket

fun main() {
    MyFirstTcpServer(InetSocketAddress("0.0.0.0", 8080))
        .run()
}

class MyFirstTcpServer(
    // The address where the server is listening to connections
    private val address: InetSocketAddress,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MyFirstTcpServer::class.java)
    }

    fun run() {
        logger.info("Server is starting")
        try {
            val serverSocket = ServerSocket()
            serverSocket.bind(address)
            val threadBuilder = Thread.ofPlatform()
            var clientCounter = 0
            while (true) {
                // accept will wait until a connection is established and will
                // return a Socket instance representing that connection
                // This is a *blocking* function because it will wait until something happens
                logger.info("Server is accepting connections on {}:{}", address.hostName, address.port)
                val clientSocket = serverSocket.accept()
                logger.info("New connection was accepted")
                val connectionId = clientCounter++
                threadBuilder.start {
                    connectionLoop(clientSocket, connectionId)
                }
            }
        } catch (ex: Exception) {
            logger.warn("Something not nice happened", ex)
        }
    }

    private fun connectionLoop(
        clientSocket: Socket,
        connectionNumber: Int,
    ) {
        clientSocket.inputStream.bufferedReader().use { reader ->
            clientSocket.outputStream.bufferedWriter().use { writer ->
                writer.writeLine("Hello, you are client $connectionNumber.")
                logger.info("Starting connection loop")
                var messageCounter = 1
                while (true) {
                    val line = reader.readLine()
                    if (line == null) {
                        logger.info("Connection was closed")
                        return
                    }
                    logger.info("New line received from the connection {}", line)
                    writer.writeLine("$messageCounter:${line.uppercase()}")
                    messageCounter += 1
                }
            }
        }
    }
}
