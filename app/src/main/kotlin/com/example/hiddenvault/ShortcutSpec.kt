package com.example.hiddenvault



/** Builds the pinned home-screen shortcut that opens the vault. */
object ShortcutSpec {

    private const val ID = "hidden_vault_open"

    fun vaultIntent(context: Context): Intent {
        return Intent(context, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    fun vault(context: Context, name: String): android.content.pm.ShortcutInfo {
        val icon = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Icon.createWithResource(context, R.mipmap.ic_launcher)
        } else {
            Icon.createWithResource(context, R.mipmap.ic_launcher)
        }
        return android.content.pm.ShortcutInfo.Builder(context, ID)
            .setShortLabel(name)
            .setLongLabel(name)
            .setIcon(icon)
            .setIntent(vaultIntent(context))
            .build()
    }
}
