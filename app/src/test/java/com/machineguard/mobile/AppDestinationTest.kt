package com.machineguard.mobile

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun `bottom navigation has the three destinations from the design`() {
        assertEquals(
            listOf(R.string.nav_dashboard, R.string.nav_alerts, R.string.nav_settings),
            AppDestination.entries.map { it.labelRes },
        )
    }
}
