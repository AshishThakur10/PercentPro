package com.atlabs.percentpro.ui.gamification

import com.atlabs.percentpro.content.GamificationState
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.model.LocalDateKey
import com.atlabs.percentpro.model.LocalDates
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import java.util.TimeZone

/**
 * How often the local date is re-sampled while a gamification screen is visible.
 *
 * A day change while the app is open must clear yesterday's question count, and
 * re-sampling on a timer is cheaper and less error-prone than asking every screen
 * to remember to reset itself. The tick stops when nothing is collecting, so it
 * costs nothing in the background.
 */
const val GAMIFICATION_DATE_TICK_MILLIS = 60_000L

/**
 * Gamification state resolved for the current local day.
 *
 * The date is re-sampled on every tick, so the snapshot is always expressed in
 * terms of today even if the stored profile was last written yesterday.
 */
fun gamificationSnapshots(
    repository: GamificationRepository,
    now: () -> Long = System::currentTimeMillis,
    timeZone: () -> TimeZone = TimeZone::getDefault
): Flow<GamificationSnapshot> =
    combine(repository.state, localDateTicks(now, timeZone)) { state, today ->
        GamificationSnapshot.from(state, today)
    }

private fun localDateTicks(
    now: () -> Long,
    timeZone: () -> TimeZone
): Flow<LocalDateKey> = flow {
    while (true) {
        // Sampled per tick, so the zone is re-read too and a traveler's time-zone
        // change moves their "today" with them.
        emit(LocalDates.fromEpochMillis(now(), timeZone()))
        delay(GAMIFICATION_DATE_TICK_MILLIS)
    }
}

/** Convenience for tests and previews that already hold a resolved day. */
fun GamificationState.snapshotFor(today: LocalDateKey): GamificationSnapshot =
    GamificationSnapshot.from(this, today)