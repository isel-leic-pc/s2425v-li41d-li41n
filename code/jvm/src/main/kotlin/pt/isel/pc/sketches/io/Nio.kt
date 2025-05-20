package pt.isel.pc.sketches.io

import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.CountDownLatch

private val logger = LoggerFactory.getLogger("nio2")

private fun logByteBuffer(bb: ByteBuffer) {
    logger.info("position:{}, limit: {}, capacity: {}", bb.position(), bb.limit(), bb.capacity())
}

object Ex0 {
    @JvmStatic
    fun main(args: Array<String>) {
        // Create
        val bb = ByteBuffer.allocate(16)
        logByteBuffer(bb)

        // Write
        bb.put(1)
        logByteBuffer(bb)
        bb.put(2)
        logByteBuffer(bb)

        // Read
        bb.flip()
        logByteBuffer(bb)
        logger.info("get(): {}", bb.get())
        logByteBuffer(bb)
        logger.info("get(): {}", bb.get())
        logByteBuffer(bb)
        try {
            logger.info("get(): {}", bb.get())
            logByteBuffer(bb)
        } catch (ex: BufferUnderflowException) {
            logger.info("BufferUnderflowException - {}", ex.message)
        }
    }
}

object Ex1 {
    @JvmStatic
    fun main(args: Array<String>) {
        val socket = AsynchronousSocketChannel.open()
        val latch = CountDownLatch(1)
        logger.info("connecting...")
        socket.connect(
            InetSocketAddress("httpbin.org2", 80),
            Unit,
            object : CompletionHandler<Void, Unit> {
                override fun completed(
                    result: Void?,
                    attachment: Unit,
                ) {
                    logger.info("connected")
                    latch.countDown()
                }

                override fun failed(
                    exc: Throwable,
                    attachment: Unit,
                ) {
                    logger.info("connect failed - {}", exc.message)
                    latch.countDown()
                }
            },
        )
        logger.info("after connect call")
        latch.await()
    }
}

object Ex2 {
    @JvmStatic
    fun main(args: Array<String>) {
        val socket = AsynchronousSocketChannel.open()
        val latch = CountDownLatch(1)
        logger.info("connecting...")
        socket.connect(
            InetSocketAddress("httpbin.org", 80),
            Unit,
            object : CompletionHandler<Void, Unit> {
                override fun completed(
                    result: Void?,
                    attachment: Unit,
                ) {
                    logger.info("connected")
                    val bytes = "GET /get HTTP/1.1\r\nHost: httpbin.org\r\nConnection: close\r\n\r\n".encodeToByteArray()
                    val writeBuffer = ByteBuffer.wrap(bytes)
                    socket.write(
                        writeBuffer,
                        Unit,
                        object : CompletionHandler<Int, Unit> {
                            override fun completed(
                                result: Int,
                                attachment: Unit?,
                            ) {
                                logger.info("write completed - {}", result)
                                val readBuffer = ByteBuffer.allocate(16)
                                socket.read(
                                    readBuffer,
                                    Unit,
                                    object : CompletionHandler<Int, Unit> {
                                        override fun completed(
                                            result: Int,
                                            attachment: Unit?,
                                        ) {
                                            readBuffer.flip()
                                            val s = String(readBuffer.array(), 0, result)
                                            logger.info("read completed - {}", s)
                                            latch.countDown()
                                        }

                                        override fun failed(
                                            exc: Throwable,
                                            attachment: Unit?,
                                        ) {
                                            logger.info("read failed - {}", exc.message)
                                            latch.countDown()
                                        }
                                    },
                                )
                            }

                            override fun failed(
                                exc: Throwable,
                                attachment: Unit?,
                            ) {
                                logger.info("write failed - {}", exc.message)
                                latch.countDown()
                            }
                        },
                    )
                }

                override fun failed(
                    exc: Throwable,
                    attachment: Unit,
                ) {
                    logger.info("connect failed - {}", exc.message)
                    latch.countDown()
                }
            },
        )
        latch.await()
    }
}
