package com.example.hiddenvault

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow

/**
 * Root screen: lock gate, app picker, hidden list and the quick-hide control.
 * Everything lives in one screen so a quick hide is always one tap away.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(vm: VaultViewModel = viewModel()) {
    val ui by vm.ui
    val context = LocalContext.current

    LaunchedEffect(Unit) { vm.load() }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(ui.vaultName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                actions = {
                    TextButton(
                        onClick = {
                            context.startActivity(Intent(context, QuickHideActivity::class.java))
                        },
                        modifier = Modifier.padding(end = 4.dp),
                    ) { Text("Quick hide") }
                },
            )
        },
    ) { padding ->
        when {
            ui.showSetup -> SetupPinBody(vm, Modifier.padding(padding))
            ui.locked -> PinPadBody(vm, Modifier.padding(padding))
            else -> VaultBody(vm, ui, Modifier.padding(padding))
        }
    }
}

@Composable
private fun SetupPinBody(vm: VaultViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Create a PIN", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Four to eight digits. You type it every time you open this screen. " +
                "If you lose the PIN the hidden list stays hidden, so keep it somewhere safe.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VaultCard {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Vault name on the icon", style = MaterialTheme.typography.labelSmall)
                Text(
                    "Use something plain like Gallery, Notes or Calculator. " +
                        "The name and icon are read from the app resources.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        PinPad(vm)
        Button(onClick = vm::savePin, enabled = ui.pin.length in 4..8) { Text("Save PIN") }
    }
}

@Composable
private fun PinPadBody(vm: VaultViewModel, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(vm.vaultLabel, style = MaterialTheme.typography.headlineSmall)
        Text(
            "Enter PIN",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .padding(vertical = 28.dp)
                .height(64.dp)
                .fillMaxWidth(0.7f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (vm.ui.pin.isEmpty()) "- - - -" else "*".repeat(vm.ui.pin.length),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Keypad(onDigit = vm::onPinDigit, onDelete = vm::deletePinDigit)
        if (vm.ui.pinError) {
            Text(
                "Wrong PIN",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 16.dp),
            )
        }
        Button(
            onClick = vm::submitPin,
            modifier = Modifier.padding(top = 20.dp),
            enabled = vm.ui.pin.length == 4 || vm.ui.pin.length >= 4,
        ) { Text("Unlock") }
        if (vm.ui.pinError) {
            OutlinedButton(
                onClick = { vm.resetVault() },
                modifier = Modifier.padding(top = 8.dp),
            ) { Text("Forgot PIN: clear the hidden list") }
        }
    }
}

@Composable
private fun VaultBody(vm: VaultViewModel, ui: VaultUi, modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VaultCard {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("${ui.hiddenCount} app(s) hidden", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Hidden here means the icon is off your home screen and this app is the " +
                        "only place left that opens it. Android 10 or newer asks you to confirm " +
                        "once when an icon is removed, and some launchers keep a copy in " +
                        "their app list. Quick hide repeats the removal instantly.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (ui.showPicker) {
            PickerBody(vm, ui)
        } else {
            Button(
                onClick = vm::openPicker,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Hide a game", modifier = Modifier.padding(start = 6.dp))
            }
        }

        Text(
            "HIDDEN",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (ui.hidden.isEmpty()) {
                item(key = "empty") {
                    VaultCard {
                        Text("Nothing hidden yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Tap Hide a game to pick the games you want off the home screen.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            } else {
                items(ui.hidden, key = { it.packageName }) { row ->
                    VaultCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    row.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            TextButton(onClick = { vm.launch(row.packageName) }) { Text("Open") }
                            TextButton(
                                onClick = { vm.unhide(row.packageName) },
                                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            ) {
                                Icon(Icons.Filled.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(" Remove", modifier = Modifier.padding(start = 4.dp))
                            }
                        }
                    }
                }
            }
            item(key = "footnote") {
                Text(
                    "Settings to try for stronger hiding",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
                )
                OutlinedButton(
                    onClick = { vm.addHomeShortcut() },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Add hidden shortcut to home screen") }
                TextButton(
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + context.packageName))
                        )
                    },
                    modifier = Modifier.padding(top = 6.dp),
                ) { Text("Open this app's system settings") }
                Text(
                    "For an app that is truly invisible in the launcher, install the vault with " +
                        "a work profile or a launcher that supports app hiding. A normal app " +
                        "cannot remove icons without a user prompt on modern Android.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp, bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
private fun PickerBody(vm: VaultViewModel, ui: VaultUi) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Tap an app to hide it",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 4.dp),
        )
        LazyColumn(modifier = Modifier.height(320.dp)) {
            items(ui.installed, key = { it.packageName }) { row ->
                if (!row.hidden) {
                    VaultCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { vm.hide(row.packageName) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(row.label, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(
                                    row.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        TextButton(onClick = vm::closePicker) { Text("Cancel") }
    }
}

@Composable
private fun Keypad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "del"),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in rows) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                for (key in row) {
                    KeypadKey(key) {
                        when (key) {
                            "" -> Unit
                            "del" -> onDelete()
                            else -> onDigit(key)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KeypadKey(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = when (label) {
                "del" -> "<-"
                else -> label
            },
            style = MaterialTheme.typography.headlineSmall,
            color = if (label.isEmpty()) Color.Transparent else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private val ui: VaultUi
    @Composable
    @Suppress("unused")
    get() = LocalContext.current.let { error("unused") }
