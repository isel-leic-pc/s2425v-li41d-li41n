package pt.isel.pc.sketches.apps

import org.slf4j.LoggerFactory
import pt.isel.pc.utils.writeLine
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Semaphore

fun main() {
    TcpServerWithConnectionLimit(InetSocketAddress("0.0.0.0", 8080))
        .run()
}

class TcpServerWithConnectionLimit(
    // The address where the server is listening to connections
    private val address: InetSocketAddress,
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TcpServerWithConnectionLimit::class.java)
    }

    fun run() {
        logger.info("Server is starting")
        try {
            val semaphore = Semaphore(3)
            val serverSocket = ServerSocket()
            serverSocket.bind(address)
            val threadBuilder = Thread.ofPlatform()
            var clientCounter = 0
            while (true) {
                // accept will wait until a connection is established and will
                // return a Socket instance representing that connection
                // This is a *blocking* function because it will wait until something happens
                logger.info("Server is accepting connections on {}:{}", address.hostName, address.port)
                // Check if a new connection can be accepted.
                // If not, WAIT until it can be accepted.
                // which will happen when a previous connection is completed
                // I.e. We have a control synchronization problem
                semaphore.acquire()
                val clientSocket = serverSocket.accept()
                logger.info("New connection was accepted")
                val connectionId = clientCounter++
                threadBuilder.start {
                    connectionLoop(clientSocket, connectionId, semaphore)
                }
            }
        } catch (ex: Exception) {
            logger.warn("Something not nice happened", ex)
        }
    }

    private fun connectionLoop(
        clientSocket: Socket,
        connectionNumber: Int,
        semaphore: Semaphore,
    ) {
        try {
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
        } finally {
            // INFORM that a connection is completed
            semaphore.release()
        }
    }
}
