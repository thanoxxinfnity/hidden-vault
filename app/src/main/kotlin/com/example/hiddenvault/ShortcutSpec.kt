package com.example.hiddenvault

import android.content.Context
import android.content.Intent
import android.os.Build

/** Builds the pinned home-screen shortcut that opens the vault. */
object ShortcutSpec {

    private const val ID = "hidden_vault_open"

    fun vaultIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.N_MR1)
    fun vault(context: Context, name: String): android.content.pm.ShortcutInfo {
        return android.content.pm.ShortcutInfo.Builder(context, ID)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.sym_def_app_icon))
            .setIntent(vaultIntent(context))
            .build()
    }
}
