package com.glowup.ai.data.repository

import com.glowup.ai.core.util.GlowResult
import com.glowup.ai.data.remote.ApiError
import com.glowup.ai.data.repository.support.RequestDeduplicator
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers ANDROID_PLAN.md 2.4 "request de-duplication" — frontend-api-map.md trap #7's
 * requirement that concurrent callers of a side-effecting GET (`/dashboard`, `/engagement`)
 * share exactly one underlying request.
 */
class RequestDeduplicatorTest {

    @Test
    fun `concurrent calls with the same key share one execution`() = runTest {
        val executions = AtomicInteger(0)
        val dedup = RequestDeduplicator<String>(TestScope(StandardTestDispatcher(testScheduler)))

        val callers = List(5) {
            async {
                dedup.run("dashboard:user-1") {
                    executions.incrementAndGet()
                    delay(100)
                    GlowResult.Success("dashboard-payload")
                }
            }
        }
        advanceUntilIdle()
        val results = callers.map { it.await() }

        assertEquals(1, executions.get())
        results.forEach { assertEquals(GlowResult.Success("dashboard-payload"), it) }
    }

    @Test
    fun `different keys never share an execution`() = runTest {
        val executions = AtomicInteger(0)
        val dedup = RequestDeduplicator<String>(TestScope(StandardTestDispatcher(testScheduler)))

        val a = async { dedup.run("dashboard:user-1") { executions.incrementAndGet(); GlowResult.Success("a") } }
        val b = async { dedup.run("dashboard:user-2") { executions.incrementAndGet(); GlowResult.Success("b") } }
        advanceUntilIdle()
        a.await()
        b.await()

        assertEquals(2, executions.get())
    }

    @Test
    fun `a fresh call after the first completes runs again`() = runTest {
        val executions = AtomicInteger(0)
        val dedup = RequestDeduplicator<String>(TestScope(StandardTestDispatcher(testScheduler)))

        dedup.run("dashboard:user-1") { executions.incrementAndGet(); GlowResult.Success("first") }
        dedup.run("dashboard:user-1") { executions.incrementAndGet(); GlowResult.Success("second") }

        assertEquals(2, executions.get())
    }

    @Test
    fun `a failure is shared with every concurrent caller too`() = runTest {
        val dedup = RequestDeduplicator<String>(TestScope(StandardTestDispatcher(testScheduler)))
        val error = ApiError.Network(RuntimeException("timeout"))

        val callers = List(3) {
            async {
                dedup.run("engagement:user-1") {
                    delay(50)
                    GlowResult.Failure(error)
                }
            }
        }
        advanceUntilIdle()
        callers.forEach { assertEquals(GlowResult.Failure(error), it.await()) }
    }
}
