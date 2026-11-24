package com.example.hiddenvault

import android.content.pm.ApplicationInfo
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** A single installed launchable app, ready to be hidden in the vault. */
data class VaultEntry(
    val packageName: String,
    val label: String,
    val iconKey: android.os.UserHandle,
)

data class AppRow(
    val packageName: String,
    val label: String,
    val hidden: Boolean,
)

/** Reads every installed app that has a launcher icon, using the LauncherApps API. */
class AppRepository(private val pm: PackageManager) {

    suspend fun loadInstalled(hiddenPackages: Set<String>): List<AppRow> = withContext(Dispatchers.IO) {
        val launcher = pm.getSystemService(LauncherApps::class.java)
        val me = android.os.Process.myUserHandle()
        val profiles = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            launcher.profiles
        } else {
            listOf(me)
        }
        val rows = LinkedHashMap<String, AppRow>()
        for (profile in profiles) {
            val items = try {
                launcher.getActivityList(null, profile)
            } catch (_: SecurityException) {
                emptyList()
            } catch (_: Exception) {
                emptyList()
            }
            for (info in items) {
                val pkg = info.applicationInfo.packageName
                if (pkg == pm.packageNameOrSelf()) continue
                if (rows.containsKey(pkg)) continue
                val label = runCatching { launcher.getApplicationLabel(pkg, profile).toString() }
                    .getOrDefault(pkg)
                rows[pkg] = AppRow(
                    packageName = pkg,
                    label = label,
                    hidden = pkg in hiddenPackages,
                )
            }
        }
        rows.values.sortedBy { it.label.lowercase() }
    }

    fun appLabel(packageName: String): String {
        return runCatching {
            val ai: ApplicationInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(ai).toString()
        }.getOrDefault(packageName)
    }
}
