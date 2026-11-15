package com.universalremote.tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalremote.tv.R
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.model.ConnectionState
import com.universalremote.tv.model.TVKey
import com.universalremote.tv.ui.components.AppGrid
import com.universalremote.tv.ui.components.ChannelControl
import com.universalremote.tv.ui.components.ColorButtons
import com.universalremote.tv.ui.components.ConnectionStatusBadge
import com.universalremote.tv.ui.components.DPad
import com.universalremote.tv.ui.components.MediaControls
import com.universalremote.tv.ui.components.NumberPad
import com.universalremote.tv.ui.components.RemoteButton
import com.universalremote.tv.ui.components.VolumeControl
import com.universalremote.tv.viewmodel.RemoteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemoteScreen(onSettings: () -> Unit, onSwitchTV: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TVRemoteApp
    val vm: RemoteViewModel = viewModel()
    val connection by vm.connection.collectAsState()
    val apps by vm.apps.collectAsState()
    val error by vm.error.collectAsState()
    val tv = vm.tv
    var showNumbers by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }
    var muted by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(tv?.id) { if (tv != null) vm.connect() }
    androidx.compose.runtime.LaunchedEffect(error) { if (error != null) showError = true }

    if (showError && error != null) {
        AlertDialog(onDismissRequest = { showError = false; vm.clearError() },
            title = { Text(stringResource(R.string.remote_message)) }, text = { Text(error.orEmpty()) },
            confirmButton = { TextButton(onClick = { showError = false; vm.clearError() }) { Text(stringResource(R.string.ok)) } })
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Column {
                Text(tv?.friendlyName ?: stringResource(R.string.remote_title), style = MaterialTheme.typography.titleLarge)
                Text(tv?.brand?.displayName ?: "", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } },
            actions = {
                ConnectionStatusBadge(connection)
                IconButton(onClick = onSwitchTV) { Text("▾") }
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
            }
        )
    }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).padding(horizontal = 14.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (connection is ConnectionState.Error || connection == ConnectionState.Disconnected) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemoteButton(stringResource(R.string.reconnect), { vm.connect() }, Modifier.weight(1f))
                    if (app.wakeOnLanEnabled) RemoteButton(stringResource(R.string.wake_up), { vm.wakeUp() }, Modifier.weight(1f))
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                RemoteButton(stringResource(R.string.power_off), { vm.powerOff() }, Modifier.weight(1f), containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                RemoteButton(stringResource(R.string.source), { vm.send(TVKey.SOURCE) }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                VolumeControl(vm::sendVolumeUp, vm::sendVolumeDown, { muted = !muted; vm.toggleMute(muted) }, Modifier.weight(1f))
                DPad({ name -> runCatching { vm.send(TVKey.valueOf(name)) } }, Modifier.weight(1.4f))
                ChannelControl(vm::sendChannelUp, vm::sendChannelDown, { vm.send(TVKey.GUIDE) }, Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RemoteButton(stringResource(R.string.back), { vm.send(TVKey.BACK) }, Modifier.weight(1f))
                RemoteButton(stringResource(R.string.home), { vm.send(TVKey.HOME) }, Modifier.weight(1f))
                RemoteButton(stringResource(R.string.menu), { vm.send(TVKey.MENU) }, Modifier.weight(1f))
            }
            MediaControls(
                onRewind = { vm.send(TVKey.REWIND) }, onPlayPause = { vm.send(TVKey.PLAY_PAUSE) },
                onFastForward = { vm.send(TVKey.FAST_FORWARD) }, onStop = { vm.send(TVKey.STOP) },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { showNumbers = !showNumbers }) {
                Text(if (showNumbers) stringResource(R.string.hide_number_pad) else stringResource(R.string.show_number_pad))
            }
            AnimatedVisibility(showNumbers) {
                NumberPad(onDigit = { number -> vm.send(TVKey.valueOf("DIGIT_$number")) }, onDelete = { vm.send(TVKey.DELETE) })
            }
            ColorButtons({ color -> runCatching { vm.send(TVKey.valueOf(color)) } }, Modifier.fillMaxWidth())
            Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.titleLarge)
            AppGrid(apps, vm::launchApp, vm::loadApps, Modifier.fillMaxWidth())
            Spacer(Modifier.padding(bottom = 18.dp))
        }
    }
}
