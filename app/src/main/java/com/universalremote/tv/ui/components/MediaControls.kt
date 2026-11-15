package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.universalremote.tv.R

@Composable
fun MediaControls(onRewind: () -> Unit, onPlayPause: () -> Unit, onFastForward: () -> Unit, onStop: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RemoteButton(stringResource(R.string.rewind), onRewind)
        RemoteButton(stringResource(R.string.play_pause), onPlayPause)
        RemoteButton(stringResource(R.string.fast_forward), onFastForward)
        RemoteButton(stringResource(R.string.stop), onStop)
    }
}
