package com.machineguard.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun `bottom navigation has the three destinations from the design`() {
        assertEquals(
            listOf("Dashboard", "Alerts", "Settings"),
            AppDestination.entries.map { it.label },
        )
    }
}
