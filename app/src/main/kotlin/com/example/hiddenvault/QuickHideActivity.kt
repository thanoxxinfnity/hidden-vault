package com.example.hiddenvault

import android.app.Activity
import android.os.Bundle
import android.widget.Toast

/**
 * Instantly drops every hidden app off the launcher and returns to the home screen.
 * Trigger it from the vault header, from a home-screen shortcut, or from a
 * Tasker-style intent: am start -n com.example.hiddenvault/.QuickHideActivity
 */
class QuickHideActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = VaultStore(this)
        val launcher = VaultLauncher(this)
        var hidden = 0
        for (pkg in store.hiddenPackages()) {
            if (launcher.hideFromLauncher(pkg)) hidden++
        }
        Toast.makeText(this, "Vault closed", Toast.LENGTH_SHORT).show()
        moveTaskToBack(true)
        finish()
        finishAndRemoveTask()
    }
}
