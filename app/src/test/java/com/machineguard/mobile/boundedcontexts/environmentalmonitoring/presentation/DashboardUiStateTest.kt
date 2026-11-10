package com.machineguard.mobile.boundedcontexts.environmentalmonitoring.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigDecimal

class DashboardUiStateTest {
    @Test
    fun `no zones produces empty instead of content`() {
        assertEquals(DashboardUiState.Empty, DashboardUiState.fromZones(emptyList()))
    }

    @Test
    fun `zones are sorted by environmental priority then name and id without mutating input`() {
        val input = listOf(
            zone("normal", "A", ZoneCondition.NORMAL),
            zone("offline", "A", ZoneCondition.OFFLINE),
            zone("no-data", "A", ZoneCondition.NO_DATA),
            zone("near", "A", ZoneCondition.NEAR_LIMIT),
            zone("critical-b", "b", ZoneCondition.OUT_OF_RANGE),
            zone("critical-a2", "A", ZoneCondition.OUT_OF_RANGE),
            zone("critical-a1", "a", ZoneCondition.OUT_OF_RANGE),
        )
        val result = DashboardUiState.fromZones(input) as DashboardUiState.Content
        assertEquals(listOf("critical-a1", "critical-a2", "critical-b", "near", "offline", "no-data", "normal"), result.zones.map { it.id })
        assertEquals("normal", input.first().id)
    }

    @Test
    fun `offline readings and missing values are preserved without averaging or replacing with zero`() {
        val readings = listOf(
            PointReadingUiModel("A", ReadingVariable.TEMPERATURE, BigDecimal("0.00"), null),
            PointReadingUiModel("B", ReadingVariable.TEMPERATURE, BigDecimal("8.75"), null),
            PointReadingUiModel("B", ReadingVariable.HUMIDITY, null, null),
        )
        val state = DashboardUiState.fromZones(listOf(zone("one", "One", ZoneCondition.OFFLINE).copy(readings = readings))) as DashboardUiState.Content
        assertEquals(readings, state.zones.single().readings)
        assertNull(state.zones.single().lastUpdatedAt)
    }

    private fun zone(id: String, name: String, condition: ZoneCondition) = ZoneCardUiModel(
        id, name, null, condition, 0, 0, 0, null, emptyList(),
    )
}
