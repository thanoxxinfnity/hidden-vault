package com.example.hiddenvault

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/** Small on-device store: the PIN hash and the list of hidden packages. */
class VaultStore(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("hidden_vault", Context.MODE_PRIVATE)
    private val gson = Gson()

    var pinHash: String?
        get() = prefs.getString(KEY_PIN, null)
        set(value) = prefs.edit().putString(KEY_PIN, value).apply()

    var displayName: String
        get() = prefs.getString(KEY_NAME, "My Gallery") ?: "My Gallery"
        set(value) = prefs.edit().putString(KEY_NAME, value).apply()

    var disguiseIcon: String
        get() = prefs.getString(KEY_ICON, "gallery") ?: "gallery"
        set(value) = prefs.edit().putString(KEY_ICON, value).apply()

    fun hiddenPackages(): Set<String> {
        val raw = prefs.getString(KEY_HIDDEN, null) ?: return emptySet()
        return runCatching {
            val type = object : TypeToken<List<String>>() {}.type
            gson.fromJson<List<String>>(raw, type)?.toSet() ?: emptySet()
        }.getOrDefault(emptySet())
    }

    fun saveHidden(list: Set<String>) {
        prefs.edit().putString(KEY_HIDDEN, gson.toJson(list.sorted())).apply()
    }

    fun hasPin(): Boolean = !pinHash.isNullOrBlank()

    fun checkPin(pin: String): Boolean {
        val stored = pinHash ?: return false
        return stored == hashPin(pin)
    }

    fun setPin(pin: String) {
        pinHash = hashPin(pin)
    }

    companion object {
        private const val KEY_PIN = "pin_hash"
        private const val KEY_HIDDEN = "hidden_packages"
        private const val KEY_NAME = "vault_name"
        private const val KEY_ICON = "vault_icon"

        /** SHA-256 with a fixed salt: the PIN never sits in prefs as plain text. */
        fun hashPin(pin: String): String {
            val md = java.security.MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(("hv$" + pin + "$vault").toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
