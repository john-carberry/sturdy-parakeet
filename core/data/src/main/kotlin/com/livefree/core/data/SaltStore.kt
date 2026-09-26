package com.livefree.core.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import com.livefree.core.security.KeyHasher

/** The per-install salt for key hashes. Created on first use and never changes. */
internal object SaltStore {
    private const val PREFS = "security"
    private const val KEY_SALT = "key_salt"

    @SuppressLint("ApplySharedPref")
    fun getOrCreate(context: Context): ByteArray {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_SALT, null)?.let { return Base64.decode(it, Base64.NO_WRAP) }
        val salt = KeyHasher.newSalt()
        // commit() rather than apply(): hashes made with this salt must never outlive it.
        prefs.edit().putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP)).commit()
        return salt
    }
}
