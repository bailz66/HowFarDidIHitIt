package com.example.howfardidihitit.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Timestamp validation tests")
class TimestampValidationTest {

    @Test
    fun `valid recent timestamp passes`() {
        val result = validateTimestamp(System.currentTimeMillis())
        assertTrue(result.isValid)
    }

    @Test
    fun `timestamp at Jan 1 2023 passes`() {
        // 1672531200000L = Jan 1 2023 00:00:00 UTC
        val result = validateTimestamp(1672531200000L)
        assertTrue(result.isValid)
    }

    @Test
    fun `timestamp just after Jan 1 2023 passes`() {
        val result = validateTimestamp(1672531200001L)
        assertTrue(result.isValid)
    }

    @Test
    fun `zero timestamp fails`() {
        val result = validateTimestamp(0L)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("positive") })
    }

    @Test
    fun `negative timestamp fails`() {
        val result = validateTimestamp(-1L)
        assertFalse(result.isValid)
    }

    @Test
    fun `timestamp before 2023 fails`() {
        // Dec 31 2022 23:59:59 UTC
        val result = validateTimestamp(1672531199000L)
        assertFalse(result.isValid)
    }

    @Test
    fun `timestamp in seconds (not milliseconds) fails`() {
        // A recent timestamp in seconds (not ms) — falls below the ms threshold
        // 1700000000 seconds ≈ Nov 2023, but detected as seconds not ms
        val result = validateTimestamp(1700000000L)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("seconds") || it.contains("before minimum") })
    }

    @Test
    fun `far future timestamp fails`() {
        // 2 minutes from now should be considered future
        val result = validateTimestamp(System.currentTimeMillis() + 120_000L)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("future") })
    }
}
