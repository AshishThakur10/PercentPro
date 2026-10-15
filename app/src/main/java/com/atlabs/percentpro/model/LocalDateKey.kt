package com.atlabs.percentpro.model

/**
 * A calendar day in the device's local time zone.
 *
 * Stored as a day number rather than an instant so that "did this happen
 * yesterday" is a subtraction, never a duration comparison. Two activities on
 * opposite sides of a clock change are on different days; two activities 23
 * hours apart may be on the same day.
 *
 * `java.time` is deliberately not used: minSdk is 23 and this module has no
 * core library desugaring, so the conversion is done with [java.util.Calendar]
 * and plain arithmetic.
 */
@JvmInline
value class LocalDateKey(val value: Int) {

    /** ISO-8601 calendar date, e.g. `2026-10-14`. Stable across locales. */
    val isoDate: String get() = LocalDates.toIso(this)

    /** Whole calendar days from this date to [other]; negative when earlier. */
    fun daysUntil(other: LocalDateKey): Int = other.value - value

    val plusOneDay: LocalDateKey get() = LocalDateKey(value + 1)
    val minusOneDay: LocalDateKey get() = LocalDateKey(value - 1)

    override fun toString(): String = isoDate

    companion object {
        /** Convenience for tests and for callers holding a known date. */
        fun of(year: Int, month: Int, day: Int): LocalDateKey =
            LocalDateKey(LocalDates.toEpochDay(year, month, day))
    }
}

object LocalDates {

    /** The local calendar day an instant falls on. */
    fun fromEpochMillis(epochMillis: Long, timeZone: java.util.TimeZone): LocalDateKey {
        val calendar = java.util.Calendar.getInstance(timeZone).apply {
            timeInMillis = epochMillis
        }
        return LocalDateKey(
            toEpochDay(
                year = calendar.get(java.util.Calendar.YEAR),
                month = calendar.get(java.util.Calendar.MONTH) + 1,
                day = calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
        )
    }

    /**
     * Days since 1970-01-01 for a proleptic Gregorian date.
     *
     * Uses the standard days-from-civil algorithm, which is exact for the whole
     * supported range and needs no lookup tables.
     */
    fun toEpochDay(year: Int, month: Int, day: Int): Int {
        val y = if (month <= 2) year - 1 else year
        val era = (if (y >= 0) y else y - 399) / 400
        val yearOfEra = y - era * 400
        val dayOfYear = (153 * (if (month > 2) month - 3 else month + 9) + 2) / 5 + day - 1
        val dayOfEra = yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear
        return era * 146097 + dayOfEra - 719468
    }

    /** Inverse of [toEpochDay], used only to render an ISO date. */
    fun toIso(date: LocalDateKey): String {
        val z = date.value + 719468
        val era = (if (z >= 0) z else z - 146096) / 146097
        val dayOfEra = z - era * 146097
        val yearOfEra =
            (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 - dayOfEra / 146096) / 365
        val year = yearOfEra + era * 400
        val dayOfYear = dayOfEra - (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100)
        val monthPrime = (5 * dayOfYear + 2) / 153
        val day = dayOfYear - (153 * monthPrime + 2) / 5 + 1
        val month = if (monthPrime < 10) monthPrime + 3 else monthPrime - 9
        val resolvedYear = if (month <= 2) year + 1 else year
        return "%04d-%02d-%02d".format(resolvedYear, month, day)
    }
}