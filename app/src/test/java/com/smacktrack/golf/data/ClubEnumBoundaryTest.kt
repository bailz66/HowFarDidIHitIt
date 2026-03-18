package com.smacktrack.golf.data

import com.smacktrack.golf.domain.Club
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Club enum boundary value tests")
class ClubEnumBoundaryTest {

    @Test
    fun `club count is exactly 18`() {
        assertEquals(18, Club.entries.size)
    }

    @Test
    fun `first club sort order is 1`() {
        val first = Club.entries.minByOrNull { it.sortOrder }
        assertNotNull(first)
        assertEquals(1, first!!.sortOrder)
    }

    @Test
    fun `last club sort order is 18`() {
        val last = Club.entries.maxByOrNull { it.sortOrder }
        assertNotNull(last)
        assertEquals(18, last!!.sortOrder)
    }

    @Test
    fun `sort orders are consecutive with no gaps`() {
        val sortOrders = Club.entries.map { it.sortOrder }.sorted()
        assertEquals((1..18).toList(), sortOrders)
    }

    @Test
    fun `no duplicate sort orders`() {
        val sortOrders = Club.entries.map { it.sortOrder }
        assertEquals(sortOrders.size, sortOrders.toSet().size)
    }

    @Test
    fun `no duplicate display names`() {
        val names = Club.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `all categories are represented`() {
        val categories = Club.entries.map { it.category }.toSet()
        assertTrue(categories.contains(Club.Category.WOOD))
        assertTrue(categories.contains(Club.Category.IRON))
        assertTrue(categories.contains(Club.Category.WEDGE))
        assertTrue(categories.contains(Club.Category.HYBRID))
    }

    @Test
    fun `driver is a wood and has sort order 1`() {
        assertEquals(Club.Category.WOOD, Club.DRIVER.category)
        assertEquals(1, Club.DRIVER.sortOrder)
        assertEquals("Driver", Club.DRIVER.displayName)
    }
}
