package com.example.hiddenvault

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.os.Build
import android.os.Process

/** Handles launching, hiding from the launcher and home-screen shortcuts. */
class VaultLauncher(private val context: Context) {

    private val launcherApps: LauncherApps? =
        context.getSystemService(LauncherApps::class.java)

    private val user: android.os.UserHandle = Process.myUserHandle()

    fun launch(packageName: String): Boolean {
        val la = launcherApps ?: return false
        return runCatching {
            val cn = ComponentName(packageName, "")
            la.startMainActivity(cn, null, user, null)
            true
        }.getOrElse {
            runCatching {
                context.startActivity(
                    context.packageManager.getLaunchIntentForPackage(packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
                true
            }.getOrDefault(false)
        }
    }

    /**
     * Removes the app icon from the launcher for this profile and brings the vault
     * to the front. On Android 10+ a normal app cannot silently hide other apps,
     * so the system confirm dialog is shown and the icon can be re-added from
     * launcher settings. What always works: the vault itself never lists the app.
     */
    fun hideFromLauncher(packageName: String): Boolean {
        val la = launcherApps ?: return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) return false
        return runCatching {
            la.hideActivity(ComponentName(packageName, ""), null, user)
            true
        }.getOrElse { false }
    }

    /** Adds a 1x1 shortcut that opens the vault directly, hidden under a plain name. */
    fun addHomeShortcut(name: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val la = launcherApps ?: return false
            return runCatching {
                val shortcut = ShortcutSpec.vault(context, name)
                la.pinShortcut(shortcut, null)
                true
            }.getOrElse { false }
        }
        val intent = Intent(Intent.ACTION_CREATE_SHORTCUT).apply {
            putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, ShortcutSpec.vaultIntent(context))
        }
        return runCatching { context.sendBroadcast(intent); true }.getOrDefault(false)
    }
}
