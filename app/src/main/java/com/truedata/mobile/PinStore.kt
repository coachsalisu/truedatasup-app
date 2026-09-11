package com.truedata.mobile

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Stores ONLY a salted hash of the PIN on the device - never the PIN itself,
 * and never sent anywhere. This is what lets the lock screen verify a PIN
 * completely offline.
 *
 * A fresh random salt is generated the first time a PIN is saved on this
 * device, and stored alongside the hash (a salt isn't secret - its only job
 * is stopping two identical PINs from ever producing the same stored hash).
 */
class PinStore(context: Context) {

    private val prefs = context.getSharedPreferences("truedata_lock", Context.MODE_PRIVATE)

    fun isPinSet(): Boolean = prefs.contains("pin_hash")

    fun savePin(pin: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hash(pin, salt)
        prefs.edit()
            .putString("pin_hash", hash)
            .putString("pin_salt", bytesToHex(salt))
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val storedHash = prefs.getString("pin_hash", null) ?: return false
        val saltHex = prefs.getString("pin_salt", null) ?: return false
        val salt = hexToBytes(saltHex)
        return hash(pin, salt) == storedHash
    }

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        val bytes = digest.digest(pin.toByteArray(Charsets.UTF_8))
        return bytesToHex(bytes)
    }

    private fun bytesToHex(bytes: ByteArray): String =
        bytes.joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray =
        ByteArray(hex.length / 2) { i -> ((Character.digit(hex[i * 2], 16) shl 4) + Character.digit(hex[i * 2 + 1], 16)).toByte() }
}
