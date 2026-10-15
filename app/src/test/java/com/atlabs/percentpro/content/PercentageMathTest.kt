package com.atlabs.percentpro.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PercentageMathTest {

    @Test
    fun `percentOf finds the part of a whole`() {
        assertEquals(20.0, PercentageMath.percentOf(25.0, 80.0), TOLERANCE)
        assertEquals(50.0, PercentageMath.percentOf(50.0, 100.0), TOLERANCE)
        assertEquals(0.0, PercentageMath.percentOf(0.0, 450.0), TOLERANCE)
        assertEquals(90.0, PercentageMath.percentOf(20.0, 450.0), TOLERANCE)
    }

    @Test
    fun `hundred percent of any quantity is the quantity itself`() {
        listOf(1.0, 7.5, 90.0, 450.0, 1280.0).forEach { whole ->
            assertEquals(whole, PercentageMath.percentOf(100.0, whole), TOLERANCE)
        }
    }

    @Test
    fun `fractionAsPercent matches the lesson three worked examples`() {
        assertEquals(50.0, PercentageMath.fractionAsPercent(1.0, 2.0), TOLERANCE)
        assertEquals(25.0, PercentageMath.fractionAsPercent(1.0, 4.0), TOLERANCE)
        assertEquals(75.0, PercentageMath.fractionAsPercent(3.0, 4.0), TOLERANCE)
        assertEquals(20.0, PercentageMath.fractionAsPercent(1.0, 5.0), TOLERANCE)
    }

    @Test
    fun `fractionAsPercent does not round thirds away`() {
        assertEquals(100.0 / 3.0, PercentageMath.fractionAsPercent(1.0, 3.0), 1e-9)
        assertTrue(PercentageMath.fractionAsPercent(1.0, 3.0) < 34.0)
        assertTrue(PercentageMath.fractionAsPercent(1.0, 3.0) > 33.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `fractionAsPercent rejects a zero denominator`() {
        PercentageMath.fractionAsPercent(1.0, 0.0)
    }

    @Test
    fun `decimalAsPercent shifts the point two places`() {
        assertEquals(25.0, PercentageMath.decimalAsPercent(0.25), TOLERANCE)
        assertEquals(40.0, PercentageMath.decimalAsPercent(0.4), TOLERANCE)
        assertEquals(100.0, PercentageMath.decimalAsPercent(1.0), TOLERANCE)
    }

    @Test
    fun `changeAsPercent measures growth against the original value`() {
        assertEquals(20.0, PercentageMath.changeAsPercent(120.0, 100.0), TOLERANCE)
        assertEquals(-20.0, PercentageMath.changeAsPercent(80.0, 100.0), TOLERANCE)
        assertEquals(50.0, PercentageMath.changeAsPercent(150.0, 100.0), TOLERANCE)
    }

    @Test
    fun `grid shades one square per percent`() {
        assertEquals(0, PercentageMath.highlightedSquareCount(0.0))
        assertEquals(25, PercentageMath.highlightedSquareCount(25.0))
        assertEquals(50, PercentageMath.highlightedSquareCount(50.0))
        assertEquals(75, PercentageMath.highlightedSquareCount(75.0))
        assertEquals(100, PercentageMath.highlightedSquareCount(100.0))
    }

    @Test
    fun `grid stays inside one hundred squares`() {
        assertEquals(100, PercentageMath.highlightedSquareCount(140.0))
        assertEquals(0, PercentageMath.highlightedSquareCount(-10.0))
        // 33.33% still lands on a whole square rather than being rejected.
        assertEquals(33, PercentageMath.highlightedSquareCount(100.0 / 3.0))
    }

    @Test
    fun `percentagesMatch compares with a tolerance`() {
        assertTrue(PercentageMath.percentagesMatch(25.0, 25.0))
        assertTrue(PercentageMath.percentagesMatch(100.0 / 3.0, 33.3333333))
        assertFalse(PercentageMath.percentagesMatch(25.0, 25.5))
    }

    private companion object {
        const val TOLERANCE = 1e-9
    }
}