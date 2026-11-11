package com.machineguard.mobile.boundedcontexts.iam

import com.machineguard.mobile.boundedcontexts.iam.domain.CredentialsValidator
import com.machineguard.mobile.boundedcontexts.iam.domain.EmailError
import com.machineguard.mobile.boundedcontexts.iam.domain.PasswordError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CredentialsValidatorTest {

    @Test
    fun `valid email has no error`() {
        assertNull(CredentialsValidator.validateEmail("demo@machineguard.com"))
    }

    @Test
    fun `email is trimmed before validating`() {
        assertNull(CredentialsValidator.validateEmail("  demo@machineguard.com  "))
    }

    @Test
    fun `empty or blank email is reported as empty`() {
        assertEquals(EmailError.Empty, CredentialsValidator.validateEmail(""))
        assertEquals(EmailError.Empty, CredentialsValidator.validateEmail("   "))
    }

    @Test
    fun `malformed emails are reported as invalid`() {
        listOf("demo", "demo@", "@machineguard.com", "demo@machineguard", "de mo@machineguard.com")
            .forEach { assertEquals(it, EmailError.Invalid, CredentialsValidator.validateEmail(it)) }
    }

    @Test
    fun `blank password is reported as empty`() {
        assertEquals(PasswordError.Empty, CredentialsValidator.validatePassword(""))
        assertEquals(PasswordError.Empty, CredentialsValidator.validatePassword("   "))
    }

    @Test
    fun `non blank password has no error`() {
        assertNull(CredentialsValidator.validatePassword("Demo1234!"))
    }
}
