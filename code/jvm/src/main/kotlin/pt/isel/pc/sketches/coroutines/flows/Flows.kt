package pt.isel.pc.sketches.coroutines.flows

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.timeout
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.milliseconds

private val logger = LoggerFactory.getLogger("flows")

object Ex0 {
    private fun createFlow() =
        flow {
            logger.info("Before emitting first value")
            emit("Hello")
            logger.info("After emitting first value")
            delay(1000)
            logger.info("Before emitting second value")
            emit("World")
            logger.info("After emitting second value")
        }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking(Dispatchers.Default) {
            logger.info("Run blocking started")
            val flow = createFlow()
            flow.collect {
                logger.info("First collector: {}", it)
                delay(500)
            }
            flow.collect {
                logger.info("Second collector: {}", it)
                delay(500)
            }
        }
    }
}
/*
 * Observations and questions:
 * - Observe that `collect`, the collector's `emit` are suspend.
 * - Use a different dispatcher and observe where the flow producer and the flow collector run.
 * - Observe the timestamp when `emit` returns, in the flow producer.
 */

object Ex1 {
    private fun createFlow() =
        flow {
            logger.info("Before emitting first value")
            emit("Hello")
            logger.info("After emitting first value")
            delay(1000)
            logger.info("Before emitting second value")
            emit("World")
            logger.info("After emitting second value")
        }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            logger.info("Run blocking started")
            val flow =
                createFlow()
                    .filter { it != "Hello" }
            flow.collect {
                logger.info("First collector: {}", it)
                delay(500)
            }
            flow.collect {
                logger.info("Second collector: {}", it)
                delay(500)
            }
        }
    }
}
/*
 * Observations and questions:
 * - Why isn't `filter` a `suspend` function?
 * - Why is the block passed to `filter` a `suspend function?
 * - Observe when the first emit (the one emitting "Hello") completes.
 */

object Ex2 {
    private fun createFlow() =
        flow {
            try {
                logger.info("Before emitting first value")
                emit("Hello")
                logger.info("After emitting first value")
                delay(1000)
                logger.info("Before emitting second value")
                emit("World")
                logger.info("After emitting second value")
            } catch (ex: Throwable) {
                logger.info("Exception caught - {} - {}", ex.javaClass.simpleName, ex.message)
            }
        }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            logger.info("Run blocking started")
            val item =
                createFlow()
                    .first {
                        logger.info("Running `first` predicate")
                        it != "World"
                    }
            logger.info("First element {}", item)
        }
    }
}
/*
 * Observations and questions:
 * - Why doesn't the builder block run until the end?
 * - Which exception is thrown?
 */

object Ex3 {
    private fun createFlow() =
        flow {
            logger.info("Before emitting first value")
            emit("Hello")
            logger.info("After emitting first value")
            delay(1000)
            logger.info("Before emitting second value")
            emit("World")
            logger.info("After emitting second value")
        }

    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            logger.info("Run blocking started")
            val flow =
                createFlow()
                    .flowOn(Dispatchers.Default)
            flow.collect {
                logger.info("First collector: {}", it)
                delay(500)
            }
            flow.collect {
                logger.info("Second collector: {}", it)
                delay(500)
            }
        }
    }
}
/*
 * Observations and questions:
 * - What runs in which thread?
 * - Why does `emit` on the builder return before `collect` running?
 * - How many coroutines are there?
 */

object Ex4 {
    private val ch =
        Channel<String>(
            Channel.UNLIMITED,
        )

    @OptIn(FlowPreview::class)
    @JvmStatic
    fun main(args: Array<String>) {
        val flow =
            ch.receiveAsFlow()
                .timeout(1000.milliseconds)
                .catch {
                    // nothing
                }
        runBlocking {
            ch.send("Hello")
            ch.send("World")
            flow.collect {
                logger.info("First collector: {}", it)
            }
            logger.info("First collect ended")
            ch.send("Olá")
            ch.send("Mundo")
            flow.collect {
                logger.info("Second collector: {}", it)
            }
            logger.info("Second collect ended")
        }
    }
}
