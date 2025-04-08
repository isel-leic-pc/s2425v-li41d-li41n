package pt.isel.pc.jmm

import org.slf4j.LoggerFactory

class A {
    val b: B = B()
}

class B {
    var c: C = C()
}

class C {
    var s: String = "hello"
}

var mutShared: A? = null

fun main() {
    val writer =
        Thread.ofPlatform().start {
            while (!Thread.interrupted()) {
                mutShared = A()
                mutShared = null
            }
        }
    val reader =
        Thread.ofPlatform().start {
            var nullObservations = 0
            var nonNullObservations = 0
            var nanos = System.nanoTime()
            while (true) {
                val observed = mutShared
                if (observed != null) {
                    // logger.info("observed not null")
                    val b = observed.b
                    if (b == null) {
                        logger.info("Race detected on b")
                        writer.interrupt()
                        break
                    }
                    val c = b.c
                    if (c == null) {
                        logger.info("Race detected on c")
                        writer.interrupt()
                        break
                    }
                    val s = c.s
                    if (s == null) {
                        logger.info("Race detected on s")
                        writer.interrupt()
                        break
                    }
                    nonNullObservations += 1
                } else {
                    nullObservations += 1
                }
                if (System.nanoTime() - nanos > 1000_000_000) {
                    logger.info("nullObservations: {}, nonNullObservations: {}", nullObservations, nonNullObservations)
                    nanos = System.nanoTime()
                }
            }
        }
    reader.join()
    writer.join()
}

private val logger = LoggerFactory.getLogger("main")
