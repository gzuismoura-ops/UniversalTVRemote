package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.universalremote.tv.R

@Composable
fun ChannelControl(onUp: () -> Unit, onDown: () -> Unit, onGuide: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        RemoteButton(stringResource(R.string.channel_up), onUp)
        RemoteButton(stringResource(R.string.channel_down), onDown)
        RemoteButton(stringResource(R.string.guide), onGuide)
    }
}
