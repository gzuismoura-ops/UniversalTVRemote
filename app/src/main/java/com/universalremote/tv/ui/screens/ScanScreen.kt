package com.universalremote.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalremote.tv.R
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.viewmodel.ScanUiState
import com.universalremote.tv.viewmodel.ScanViewModel
import com.universalremote.tv.ui.components.TVCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(onSelect: (DiscoveredTV) -> Unit, onSettings: () -> Unit) {
    val vm: ScanViewModel = viewModel()
    val state by vm.state.collectAsState()
    val savedIps = vm.savedDevices().map { it.ip }.toSet()
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) }, actions = {
                IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings)) }
            })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { vm.scan() }) { Text("↻") }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            when (val current = state) {
                ScanUiState.Idle -> Text(stringResource(R.string.scan_intro), modifier = Modifier.padding(top = 18.dp))
                is ScanUiState.Scanning -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        CircularProgressIndicator()
                        Text(current.message)
                    }
                }
                is ScanUiState.Error -> Text(current.message)
                is ScanUiState.Ready -> {
                    Text(stringResource(R.string.devices_found, current.devices.size))
                    if (current.devices.isEmpty()) Text(stringResource(R.string.no_devices))
                    LazyColumn(contentPadding = PaddingValues(bottom = 90.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(current.devices, key = { it.id }) { device -> TVCard(device, device.ip in savedIps, { onSelect(device) }) }
                    }
                }
            }
        }
    }
}
