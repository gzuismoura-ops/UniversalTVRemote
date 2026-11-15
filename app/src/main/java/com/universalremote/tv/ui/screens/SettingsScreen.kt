package com.universalremote.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.ui.components.RemoteButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TVRemoteApp
    var darkMode by remember { mutableStateOf(app.darkMode.value) }
    var wolEnabled by remember { mutableStateOf(app.wakeOnLanEnabled) }
    var saved by remember { mutableStateOf(app.credentialStore.getAllSavedTVs()) }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.settings)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.dark_theme), Modifier.weight(1f))
                Switch(checked = darkMode, onCheckedChange = { darkMode = it; app.setDarkMode(it) })
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.wol_setting), Modifier.weight(1f))
                Switch(checked = wolEnabled, onCheckedChange = { wolEnabled = it; app.wakeOnLanEnabled = it })
            }
            HorizontalDivider()
            Text(stringResource(R.string.saved_tvs), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            if (saved.isEmpty()) Text(stringResource(R.string.no_saved_tvs))
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(saved, key = { it.ip }) { device ->
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(device.friendlyName)
                            Text("${device.brand.displayName} · ${device.ip}", style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                        }
                        RemoteButton(stringResource(R.string.delete), {
                            app.credentialStore.deleteTVCredentials(device.ip)
                            saved = app.credentialStore.getAllSavedTVs()
                        })
                    }
                }
            }
            HorizontalDivider()
            Text(stringResource(R.string.about_title), style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.about_text), style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
            RemoteButton(stringResource(R.string.back), onBack, Modifier.fillMaxWidth())
        }
    }
}
