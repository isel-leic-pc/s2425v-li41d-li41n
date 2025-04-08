package pt.isel.pc.synchronizers

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class VerySimpleThreadPool(
    private val maxThreads: Int,
) {
    init {
        require(maxThreads > 0)
    }

    private val mutex = ReentrantLock()
    private var numberOfWorkerThreads = 0
    private val workItems = mutableListOf<Runnable>()

    fun execute(runnable: Runnable) {
        mutex.withLock {
            if (numberOfWorkerThreads < maxThreads) {
                // create a new worker thread
                numberOfWorkerThreads += 1
                Thread.ofPlatform().start {
                    workerThreadLoop(runnable)
                }
            } else {
                // add the work item to the queue
                workItems.addLast(runnable)
            }
        }
    }

    private fun workerThreadLoop(initialRunnable: Runnable) {
        var currentRunnable = initialRunnable
        while (true) {
            try {
                currentRunnable.run()
            } catch (ex: Throwable) {
                // ignore exceptions
                // TODO log exception
            }
            // clear the interrupt status
            Thread.interrupted()
            currentRunnable = getNextWorkItem()
                ?: break
        }
    }

    private fun getNextWorkItem(): Runnable? {
        mutex.withLock {
            if (workItems.isEmpty()) {
                numberOfWorkerThreads -= 1
                return null
            } else {
                return workItems.removeFirst()
            }
        }
    }
}
