package com.machineguard.mobile.boundedcontexts.environmentalmonitoring

import com.machineguard.mobile.boundedcontexts.environmentalmonitoring.domain.ZoneCondition
import org.junit.Assert.assertEquals
import org.junit.Test

class ZoneConditionTest {

    @Test
    fun `the four values sent by the Core API map to their conditions`() {
        assertEquals(ZoneCondition.NORMAL, ZoneCondition.fromApi("NORMAL"))
        assertEquals(ZoneCondition.OUT_OF_RANGE, ZoneCondition.fromApi("OUT_OF_RANGE"))
        assertEquals(ZoneCondition.STALE, ZoneCondition.fromApi("STALE"))
        assertEquals(ZoneCondition.NO_DATA, ZoneCondition.fromApi("NO_DATA"))
    }

    @Test
    fun `values the Core never sends and unknown values map to UNKNOWN`() {
        assertEquals(ZoneCondition.UNKNOWN, ZoneCondition.fromApi("NEAR_LIMIT"))
        assertEquals(ZoneCondition.UNKNOWN, ZoneCondition.fromApi("OFFLINE"))
        assertEquals(ZoneCondition.UNKNOWN, ZoneCondition.fromApi("ALGO_NUEVO"))
        assertEquals(ZoneCondition.UNKNOWN, ZoneCondition.fromApi(null))
    }
}
