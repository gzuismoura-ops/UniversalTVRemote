package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R

@Composable
fun ColorButtons(onColor: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        RemoteButton(stringResource(R.string.red), { onColor("RED") }, Modifier.weight(1f), containerColor = Color(0xFFC62828), contentColor = Color.White)
        RemoteButton(stringResource(R.string.green), { onColor("GREEN") }, Modifier.weight(1f), containerColor = Color(0xFF2E7D32), contentColor = Color.White)
        RemoteButton(stringResource(R.string.yellow), { onColor("YELLOW") }, Modifier.weight(1f), containerColor = Color(0xFFF9A825), contentColor = Color.Black)
        RemoteButton(stringResource(R.string.blue), { onColor("BLUE") }, Modifier.weight(1f), containerColor = Color(0xFF1565C0), contentColor = Color.White)
    }
}
