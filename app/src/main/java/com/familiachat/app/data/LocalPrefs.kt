package com.familiachat.app.data

import android.content.Context
import java.security.MessageDigest

/**
 * Guarda o pareamento (familyId) e o hash do PIN localmente no aparelho.
 * Nada disso é sincronizado entre os dois celulares.
 */
class LocalPrefs(context: Context) {

    private val prefs = context.getSharedPreferences("familia_chat_prefs", Context.MODE_PRIVATE)

    var familyId: String?
        get() = prefs.getString(KEY_FAMILY_ID, null)
        set(value) = prefs.edit().putString(KEY_FAMILY_ID, value).apply()

    var displayName: String?
        get() = prefs.getString(KEY_DISPLAY_NAME, null)
        set(value) = prefs.edit().putString(KEY_DISPLAY_NAME, value).apply()

    private var pinHash: String?
        get() = prefs.getString(KEY_PIN_HASH, null)
        set(value) = prefs.edit().putString(KEY_PIN_HASH, value).apply()

    val hasPin: Boolean
        get() = pinHash != null

    fun setPin(pin: String) {
        pinHash = sha256(pin)
    }

    fun checkPin(pin: String): Boolean {
        return pinHash == sha256(pin)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun sha256(value: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_FAMILY_ID = "family_id"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_DISPLAY_NAME = "display_name"
    }
}
