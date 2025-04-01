package pt.isel.pc.synchronizers41n

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VerySimpleThreadPool(
    private val maxThreads: Int,
) {
    private val mutex = ReentrantLock()
    private val workItems = mutableListOf<Runnable>()
    private var nOfWorkerThreads = 0

    fun execute(runnable: Runnable) =
        mutex.withLock {
            if (nOfWorkerThreads < maxThreads) {
                Thread.ofPlatform().start {
                    workerThreadLoop(runnable)
                }
                nOfWorkerThreads += 1
            } else {
                workItems.addLast(runnable)
            }
        }

    private fun workerThreadLoop(firstRunnable: Runnable) {
        TODO("")
    }
}
