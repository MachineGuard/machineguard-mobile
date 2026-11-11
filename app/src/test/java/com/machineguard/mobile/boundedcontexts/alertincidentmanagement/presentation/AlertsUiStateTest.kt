package com.machineguard.mobile.boundedcontexts.alertincidentmanagement.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.OffsetDateTime

class AlertsUiStateTest {
    @Test
    fun `empty repository result produces empty state`() {
        assertEquals(AlertsUiState.Empty, AlertsUiState.fromAlerts(emptyList()))
    }

    @Test
    fun `active includes pending and escalated but excludes acknowledged and closed`() {
        assertEquals(listOf("alert-0", "alert-2"), AlertFilter.ACTIVE.select(alerts()).map { it.id })
    }

    @Test
    fun `acknowledged excludes other statuses and all includes closed`() {
        val alerts = alerts()
        assertEquals(listOf("alert-1"), AlertFilter.ACKNOWLEDGED.select(alerts).map { it.id })
        assertEquals(alerts, AlertFilter.ALL.select(alerts))
    }

    @Test
    fun `filter with no matches does not discard the full content`() {
        val source = listOf(alerts().last())
        val state = AlertsUiState.fromAlerts(source) as AlertsUiState.Content
        assertTrue(AlertFilter.ACTIVE.select(state.alerts).isEmpty())
        assertEquals(source, AlertFilter.ALL.select(state.alerts))
    }

    @Test
    fun `snapshot keeps source order timestamps and distinct incident identity`() {
        val source = alerts().toMutableList()
        val expected = source.toList()
        val state = AlertsUiState.fromAlerts(source) as AlertsUiState.Content
        source.clear()
        assertEquals(expected, state.alerts)
        state.alerts.forEach { assertNotEquals(it.id, it.incidentId) }
        assertEquals(expected.filter { it.status == AlertStatus.PENDING || it.status == AlertStatus.ESCALATED },
            AlertFilter.ACTIVE.select(state.alerts))
    }

    private fun alerts(): List<AlertCardUiModel> = AlertStatus.entries.mapIndexed { index, status ->
        AlertCardUiModel(
            id = "alert-$index", incidentId = "incident-$index", severity = AlertSeverity.HIGH,
            message = "Test alert", status = status,
            raisedAt = OffsetDateTime.parse("2026-11-10T10:15:00-05:00").minusHours(index.toLong()),
            acknowledgedAt = null, escalatedAt = null,
        )
    }
}
