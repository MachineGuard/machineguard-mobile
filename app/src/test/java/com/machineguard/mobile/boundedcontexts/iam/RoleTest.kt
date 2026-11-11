package com.machineguard.mobile.boundedcontexts.iam

import com.machineguard.mobile.boundedcontexts.iam.domain.Role
import org.junit.Assert.assertEquals
import org.junit.Test

class RoleTest {

    @Test
    fun `known roles are parsed ignoring case`() {
        assertEquals(Role.ADMIN, Role.fromApi("ADMIN"))
        assertEquals(Role.VIEWER, Role.fromApi("viewer"))
    }

    @Test
    fun `unknown or missing role falls back to least privilege`() {
        assertEquals(Role.VIEWER, Role.fromApi("SUPERUSER"))
        assertEquals(Role.VIEWER, Role.fromApi(null))
    }
}
