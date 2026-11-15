package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R

@Composable
fun VolumeControl(onUp: () -> Unit, onDown: () -> Unit, onMute: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RemoteButton(stringResource(R.string.volume_up), onUp)
        RemoteButton(stringResource(R.string.volume_down), onDown)
        RemoteButton(stringResource(R.string.mute), onMute)
    }
}
