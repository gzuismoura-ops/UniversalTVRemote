package com.universalremote.tv.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.universalremote.tv.R
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.model.ConnectionState
import com.universalremote.tv.model.TVBrand
import com.universalremote.tv.ui.components.PinInputField
import com.universalremote.tv.ui.components.RemoteButton
import com.universalremote.tv.viewmodel.PairViewModel
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairScreen(onPaired: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val app = context.applicationContext as TVRemoteApp
    val tv = app.selectedTv
    val vm: PairViewModel = viewModel()
    val state by vm.state.collectAsState()
    var pin by remember { mutableStateOf("") }
    val needsPin = tv?.brand == TVBrand.SONY || tv?.brand == TVBrand.VIZIO || tv?.brand == TVBrand.PHILIPS
    val instruction = when (tv?.brand) {
        TVBrand.LG -> stringResource(R.string.pair_lg)
        TVBrand.SAMSUNG -> stringResource(R.string.pair_samsung)
        TVBrand.SONY -> stringResource(R.string.pair_sony)
        TVBrand.VIZIO -> stringResource(R.string.pair_vizio)
        TVBrand.ANDROID_TV -> stringResource(R.string.pair_android)
        TVBrand.PHILIPS -> stringResource(R.string.pair_philips)
        else -> stringResource(R.string.pair_generic)
    }
    androidx.compose.runtime.LaunchedEffect(state) { if (state == ConnectionState.Connected) onPaired() }
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.pairing_title)) }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Text(tv?.brand?.emoji ?: "📺", style = MaterialTheme.typography.headlineLarge)
            Text(tv?.friendlyName ?: stringResource(R.string.no_tv_selected), style = MaterialTheme.typography.headlineMedium)
            Text(instruction, style = MaterialTheme.typography.bodyLarge)
            if (needsPin) PinInputField(pin, { pin = it })
            when (state) {
                ConnectionState.Connecting -> CircularProgressIndicator()
                is ConnectionState.Error -> Text((state as ConnectionState.Error).message, color = MaterialTheme.colorScheme.error)
                else -> Unit
            }
            if (needsPin) {
                RemoteButton(stringResource(R.string.start_pairing), { vm.pair(null) })
                RemoteButton(stringResource(R.string.submit_pin), { vm.pair(pin) }, enabled = pin.isNotBlank())
            } else {
                RemoteButton(stringResource(R.string.start_pairing), { vm.pair(null) })
            }
            RemoteButton(stringResource(R.string.back), onBack)
        }
    }
}
