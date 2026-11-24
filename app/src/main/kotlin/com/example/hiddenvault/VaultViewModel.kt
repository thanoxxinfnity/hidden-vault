package com.example.hiddenvault

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch


data class VaultUi(
    val locked: Boolean = true,
    val showSetup: Boolean = false,
    val pin: String = "",
    val pinError: Boolean = false,
    val vaultName: String = "Gallery",
    val hiddenCount: Int = 0,
    val hidden: List<AppRow> = emptyList(),
    val installed: List<AppRow> = emptyList(),
    val showPicker: Boolean = false,
)

class VaultViewModel(app: Application) : AndroidViewModel(app) {

    private val store = VaultStore(app)
    private val launcher = VaultLauncher(app)
    private val repo = AppRepository(app.packageManager)

    var ui by mutableStateOf(VaultUi())
        private set

    val vaultLabel: String get() = ui.vaultName

    fun load() {
        val hasPin = store.hasPin()
        ui = ui.copy(
            showSetup = !hasPin,
            locked = hasPin,
            vaultName = store.displayName,
        )
        if (!hasPin) return
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val hidden = store.hiddenPackages()
            val rows = repo.loadInstalled(hidden)
            ui = ui.copy(
                hidden = rows.filter { it.hidden },
                installed = rows,
                hiddenCount = rows.count { it.hidden },
            )
        }
    }

    fun onPinDigit(d: String) {
        if (ui.pin.length < 8) ui = ui.copy(pin = ui.pin + d, pinError = false)
    }

    fun deletePinDigit() {
        if (ui.pin.isNotEmpty()) ui = ui.copy(pin = ui.pin.dropLast(1))
    }

    fun submitPin() {
        if (store.checkPin(ui.pin)) {
            ui = ui.copy(locked = false, pin = "", pinError = false)
            refresh()
        } else {
            ui = ui.copy(pinError = true, pin = "")
        }
    }

    fun savePin() {
        if (ui.pin.length in 4..8) {
            store.setPin(ui.pin)
            ui = ui.copy(locked = false, showSetup = false, pin = "", pinError = false)
            refresh()
        }
    }

    fun resetVault() {
        store.pinHash = null
        store.saveHidden(emptySet())
        ui = VaultUi(showSetup = true, locked = true, vaultName = store.displayName)
    }

    fun openPicker() { ui = ui.copy(showPicker = true) }
    fun closePicker() { ui = ui.copy(showPicker = false) }

    fun hide(pkg: String) {
        val set = store.hiddenPackages().toMutableSet().apply { add(pkg) }
        store.saveHidden(set)
        launcher.hideFromLauncher(pkg)
        refresh()
        ui = ui.copy(showPicker = false)
    }

    fun unhide(pkg: String) {
        val set = store.hiddenPackages().toMutableSet().apply { remove(pkg) }
        store.saveHidden(set)
        refresh()
    }

    fun launch(pkg: String) {
        if (!launcher.launch(pkg)) {
            android.widget.Toast.makeText(getApplication(), "Could not open that app", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun addHomeShortcut() {
        val ok = launcher.addHomeShortcut(store.displayName)
        if (ok) {
            android.widget.Toast.makeText(getApplication(), "Shortcut added. Tap it to jump straight into the vault.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }
}
