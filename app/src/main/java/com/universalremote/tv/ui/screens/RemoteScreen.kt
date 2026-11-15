package com.universalremote.tv.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
            title = { Text(stringResource(R.string.remote_message)) },
            text = { Text(error.orEmpty()) },
            confirmButton = { TextButton(onClick = { showError = false; vm.clearError() }) { Text(stringResource(R.string.ok)) } })
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(tv?.friendlyName ?: stringResource(R.string.remote_title), fontWeight = FontWeight.Bold)
                        Text("${tv?.brand?.displayName.orEmpty()}  •  ${tv?.ip.orEmpty()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    ConnectionStatusBadge(connection)
                    IconButton(onClick = onSwitchTV) { Text("▾", style = MaterialTheme.typography.titleLarge) }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Device hero / connection card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Box(Modifier.background(Brush.linearGradient(listOf(Color(0xFF7555D9), Color(0xFF20B8AE))), CircleShape)
                        .padding(15.dp)) { Text("TV", color = Color.White, fontWeight = FontWeight.ExtraBold) }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Seu controle remoto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(when (connection) {
                            ConnectionState.Connected -> "TV conectada e pronta para comandos"
                            ConnectionState.Connecting -> "Conectando e aguardando autorização…"
                            is ConnectionState.Error -> "Não foi possível conectar à TV"
                            else -> "Conecte à TV na mesma rede Wi-Fi"
                        }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (connection != ConnectionState.Connected) {
                        RemoteButton(stringResource(R.string.reconnect), { vm.connect() })
                    }
                }
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                RemoteButton("⏻  ${stringResource(R.string.power_off)}", { vm.powerOff() }, Modifier.weight(1f),
                    containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
                RemoteButton("▣  ${stringResource(R.string.source)}", { vm.send(TVKey.SOURCE) }, Modifier.weight(1f))
            }

            // Directional pad in its own visual panel
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(vertical = 18.dp, horizontal = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("NAVEGAÇÃO", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    DPad({ name -> runCatching { vm.send(TVKey.valueOf(name)) } }, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        RemoteButton("↶  ${stringResource(R.string.back)}", { vm.send(TVKey.BACK) }, Modifier.weight(1f))
                        RemoteButton("⌂  ${stringResource(R.string.home)}", { vm.send(TVKey.HOME) }, Modifier.weight(1f),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                        RemoteButton("☰  ${stringResource(R.string.menu)}", { vm.send(TVKey.MENU) }, Modifier.weight(1f))
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("VOLUME E CANAIS", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        VolumeControl(vm::sendVolumeUp, vm::sendVolumeDown,
                            { muted = !muted; vm.toggleMute(muted) }, Modifier.weight(1f))
                        ChannelControl(vm::sendChannelUp, vm::sendChannelDown,
                            { vm.send(TVKey.GUIDE) }, Modifier.weight(1f))
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("CONTROLES DE MÍDIA", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    MediaControls(
                        onRewind = { vm.send(TVKey.REWIND) }, onPlayPause = { vm.send(TVKey.PLAY_PAUSE) },
                        onFastForward = { vm.send(TVKey.FAST_FORWARD) }, onStop = { vm.send(TVKey.STOP) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("TECLADO NUMÉRICO", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                            Text("Digite canais e números", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        TextButton(onClick = { showNumbers = !showNumbers }) {
                            Text(if (showNumbers) "Ocultar" else "Mostrar")
                        }
                    }
                    AnimatedVisibility(showNumbers) {
                        NumberPad(onDigit = { number -> vm.send(TVKey.valueOf("DIGIT_$number")) },
                            onDelete = { vm.send(TVKey.DELETE) })
                    }
                }
            }

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("ATALHOS COLORIDOS", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold)
                    ColorButtons({ color -> runCatching { vm.send(TVKey.valueOf(color)) } }, Modifier.fillMaxWidth())
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Aplicativos disponíveis na TV", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = { vm.loadApps() }) { Text("Atualizar") }
            }
            AppGrid(apps, vm::launchApp, vm::loadApps, Modifier.fillMaxWidth())
            if (app.wakeOnLanEnabled) {
                RemoteButton("Ligar TV pela rede", { vm.wakeUp() }, Modifier.fillMaxWidth())
            }
            Spacer(Modifier.height(18.dp))
        }
    }
}
