package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R

@Composable
fun DPad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RemoteButton(stringResource(R.string.key_up), { onKey("UP") }, Modifier.size(66.dp, 52.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RemoteButton(stringResource(R.string.key_left), { onKey("LEFT") }, Modifier.size(66.dp, 56.dp))
            RemoteButton(stringResource(R.string.key_ok), { onKey("OK") }, Modifier.size(66.dp, 56.dp))
            RemoteButton(stringResource(R.string.key_right), { onKey("RIGHT") }, Modifier.size(66.dp, 56.dp))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            RemoteButton(stringResource(R.string.key_down), { onKey("DOWN") }, Modifier.size(66.dp, 52.dp))
        }
    }
}
