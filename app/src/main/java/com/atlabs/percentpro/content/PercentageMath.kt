package com.atlabs.percentpro.content

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * The arithmetic used by the lesson content.
 *
 * Every number a lesson shows the learner is produced here rather than typed into
 * a string, so the worked examples cannot drift away from the maths and can be
 * asserted in unit tests.
 */
object PercentageMath {

    const val WHOLE_SQUARES = 100

    /** `percentOf(25, 80) == 20` — a quarter of 80. */
    fun percentOf(percent: Double, whole: Double): Double = whole * percent / 100.0

    /** `fractionAsPercent(1, 4) == 25.0` — exact, not rounded. */
    fun fractionAsPercent(numerator: Double, denominator: Double): Double {
        require(denominator != 0.0) { "Denominator must not be zero" }
        return numerator * 100.0 / denominator
    }

    /** `decimalAsPercent(0.25) == 25.0` */
    fun decimalAsPercent(decimal: Double): Double = decimal * 100.0

    /** `changeAsPercent(20, 100) == 20.0` — percentage growth from old to new. */
    fun changeAsPercent(newValue: Double, oldValue: Double): Double {
        require(oldValue != 0.0) { "Reference value must not be zero" }
        return (newValue - oldValue) * 100.0 / oldValue
    }

    /**
     * How many of the 100 squares in the lesson 2 grid are shaded for [percent].
     * Rounded so that values like 33.33% still land on a whole square instead of
     * being rejected.
     */
    fun highlightedSquareCount(percent: Double): Int =
        (percent * WHOLE_SQUARES / 100.0).roundToLong()
            .coerceIn(0L, WHOLE_SQUARES.toLong())
            .toInt()

    /**
     * True when two percentages describe the same quantity. Used by the lesson 3
     * content to confirm a converted fraction matches the expected value.
     */
    fun percentagesMatch(a: Double, b: Double, tolerance: Double = 1e-6): Boolean =
        abs(a - b) <= tolerance
}