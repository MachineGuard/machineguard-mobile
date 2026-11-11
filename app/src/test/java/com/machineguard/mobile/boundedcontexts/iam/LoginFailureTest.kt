package com.machineguard.mobile.boundedcontexts.iam

import com.machineguard.mobile.boundedcontexts.iam.application.LoginFailure
import com.machineguard.mobile.boundedcontexts.iam.application.toLoginFailure
import com.machineguard.mobile.core.data.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginFailureTest {

    @Test
    fun `credentials and rate limiting map to their own messages`() {
        assertEquals(LoginFailure.InvalidCredentials, AppError.InvalidCredentials.toLoginFailure())
        assertEquals(LoginFailure.TooManyRequests, AppError.TooManyRequests(retryAfterSeconds = 30).toLoginFailure())
    }

    @Test
    fun `connectivity problems map to the network message`() {
        assertEquals(LoginFailure.Network, AppError.Offline.toLoginFailure())
        assertEquals(LoginFailure.Network, AppError.Timeout.toLoginFailure())
    }

    @Test
    fun `server and forbidden responses map to server and disabled account messages`() {
        assertEquals(LoginFailure.Server, AppError.Server(statusCode = 503).toLoginFailure())
        assertEquals(LoginFailure.AccountDisabled, AppError.Forbidden().toLoginFailure())
    }

    @Test
    fun `anything else falls back to the generic message`() {
        assertEquals(LoginFailure.Unknown, AppError.Unknown().toLoginFailure())
        assertEquals(LoginFailure.Unknown, AppError.Invalid().toLoginFailure())
        assertEquals(LoginFailure.Unknown, AppError.SessionExpired.toLoginFailure())
    }
}
