package com.machineguard.mobile.core.session

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.io.IOException
import java.security.GeneralSecurityException
import java.time.Instant

/**
 * Guarda la sesión en [EncryptedSharedPreferences]: claves y valores cifrados con una llave AES-256 del Keystore de
 * Android, que nunca sale del dispositivo. Los tokens no se escriben en logs ni en archivos de texto plano.
 *
 * Si el Keystore ya no puede descifrar los datos (restauración de un respaldo, cambio de huella, llave invalidada),
 * se descarta la sesión guardada y la app pide iniciar sesión de nuevo, en lugar de fallar al abrir.
 */
class EncryptedSessionStorage(private val context: Context) : SessionStorage {

    private val prefs: SharedPreferences by lazy { openOrReset() }

    override fun readTokens(): AuthTokens? = safely {
        val access = prefs.getString(KEY_ACCESS, null)
        val refresh = prefs.getString(KEY_REFRESH, null)
        if (access.isNullOrEmpty() || refresh.isNullOrEmpty()) {
            null
        } else {
            val expiresAtMillis = prefs.getLong(KEY_EXPIRES_AT, NO_EXPIRY)
            AuthTokens(access, refresh, if (expiresAtMillis == NO_EXPIRY) null else Instant.ofEpochMilli(expiresAtMillis))
        }
    }

    override fun writeTokens(tokens: AuthTokens) {
        prefs.edit()
            .putString(KEY_ACCESS, tokens.accessToken)
            .putString(KEY_REFRESH, tokens.refreshToken)
            .putLong(KEY_EXPIRES_AT, tokens.accessExpiresAt?.toEpochMilli() ?: NO_EXPIRY)
            .apply()
    }

    override fun readProfileJson(): String? = safely { prefs.getString(KEY_PROFILE, null) }

    override fun writeProfileJson(json: String) {
        prefs.edit().putString(KEY_PROFILE, json).apply()
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }

    private fun <T> safely(block: () -> T?): T? = try {
        block()
    } catch (e: GeneralSecurityException) {
        resetStorage()
        null
    } catch (e: IOException) {
        resetStorage()
        null
    }

    private fun openOrReset(): SharedPreferences = try {
        create()
    } catch (e: GeneralSecurityException) {
        resetStorage()
        create()
    } catch (e: IOException) {
        resetStorage()
        create()
    }

    // security-crypto 1.0.0: la llave maestra se identifica por su alias en el Keystore.
    @Suppress("DEPRECATION")
    private fun create(): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            FILE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    private fun resetStorage() {
        context.deleteSharedPreferences(FILE_NAME)
    }

    private companion object {
        const val FILE_NAME = "machineguard_session"
        const val KEY_ACCESS = "access_token"
        const val KEY_REFRESH = "refresh_token"
        const val KEY_EXPIRES_AT = "access_expires_at"
        const val KEY_PROFILE = "profile"
        const val NO_EXPIRY = -1L
    }
}
