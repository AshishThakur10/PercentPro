package com.atlabs.percentpro.content

import com.atlabs.percentpro.data.BaselineMarkerStore
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.GamificationStore
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.LocalDates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.TimeZone

/**
 * In-memory [GamificationStore] with the same atomic-update contract as the
 * DataStore one, so repository tests exercise real read-modify-write behaviour
 * without an Android device.
 */
class InMemoryGamificationStore(initial: String? = null) : GamificationStore {

    private val mutex = Mutex()
    private val state = MutableStateFlow(initial)

    /** Counts how many times a value was written, for write-amplification checks. */
    var writeCount: Int = 0
        private set

    override val payload: Flow<String?> = state

    override suspend fun read(): String? = mutex.withLock { state.value }

    override suspend fun update(transform: (String?) -> String?) {
        mutex.withLock {
            val updated = transform(state.value)
            if (updated != state.value) {
                writeCount++
                state.value = updated
            }
        }
    }

    /** Simulates a corrupted or hand-edited file. */
    suspend fun corruptWith(raw: String) {
        mutex.withLock { state.value = raw }
    }
}

/**
 * In-memory baseline marker.
 *
 * Public so a test in another file can build a repository over shared storage
 * without duplicating the fixture.
 */
class InMemoryBaselineMarker : BaselineMarkerStore {

    private val state = MutableStateFlow(false)

    override val seeded: Flow<Boolean> = state

    override suspend fun isSeeded(): Boolean = state.value

    override suspend fun markSeeded() {
        state.value = true
    }

    override suspend fun clear() {
        state.value = false
    }
}

/** Fixed clock so no test depends on the real current date. */
class TestClock(
    var epochMillis: Long,
    private val zone: TimeZone = TimeZone.getTimeZone("Asia/Kolkata")
) {
    fun today(): LocalDateKey = LocalDates.fromEpochMillis(epochMillis, zone)

    fun advanceDays(days: Int) {
        epochMillis += days * MILLIS_PER_DAY
    }

    companion object {
        const val MILLIS_PER_DAY = 24L * 60 * 60 * 1000

        /** 2026-01-15 10:00 IST. */
        fun starting() = TestClock(1_768_407_000_000L)
    }
}

/** Convenience builder for a repository wired to a fixed clock. */
fun testRepository(
    store: GamificationStore = InMemoryGamificationStore(),
    baseline: BaselineMarkerStore = InMemoryBaselineMarker(),
    clock: TestClock = TestClock.starting()
): GamificationRepository = GamificationRepository(
    store = store,
    baselineMarker = baseline,
    now = { clock.epochMillis },
    timeZone = { TimeZone.getTimeZone("Asia/Kolkata") }
)