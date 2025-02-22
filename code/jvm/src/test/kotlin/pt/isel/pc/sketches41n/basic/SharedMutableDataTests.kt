package pt.isel.pc.sketches41n.basic

import org.junit.jupiter.api.Assertions.assertNotEquals
import kotlin.test.Test

class SharedMutableDataTests {
    private var sharedCounter = 0

    @Test
    fun someTest() {
        // when: N_OF_THREADS threads do N_OF_REPS increments on a shared counter
        val threadBuilder = Thread.ofPlatform()
        val ths =
            List(N_OF_THREADS) {
                threadBuilder.start {
                    repeat(N_OF_REPS) {
                        sharedCounter += 1
                    }
                }
            }
        ths.forEach { it.join() }
        // then: the final counter value is NOT N_OF_THREADS * N_OF_REPS
        assertNotEquals(N_OF_THREADS * N_OF_REPS, sharedCounter)
    }

    companion object {
        const val N_OF_THREADS = 10
        const val N_OF_REPS = 10_000
    }
}
