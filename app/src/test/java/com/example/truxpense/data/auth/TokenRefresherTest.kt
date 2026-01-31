package com.example.truxpense.data.auth

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.junit.Assert.assertEquals
import org.junit.Test

class SingleFlightTest {
    @Test
    fun concurrentCallsInvokeBlockOnce() = runBlocking {
        var calls = 0

        val sf = SingleFlight<Int>()

        suspend fun work(): Int {
            calls++
            // simulate work
            delay(100)
            return 42
        }

        val d1 = async { sf.run { work() } }
        val d2 = async { sf.run { work() } }
        val d3 = async { sf.run { work() } }

        val r1 = d1.await()
        val r2 = d2.await()
        val r3 = d3.await()

        assertEquals(42, r1)
        assertEquals(42, r2)
        assertEquals(42, r3)
        assertEquals(1, calls)
    }
}
