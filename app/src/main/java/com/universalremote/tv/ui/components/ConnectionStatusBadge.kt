package com.universalremote.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R
import com.universalremote.tv.model.ConnectionState

@Composable
fun ConnectionStatusBadge(state: ConnectionState, modifier: Modifier = Modifier) {
    val label = when (state) {
        ConnectionState.Connected -> stringResource(R.string.connected)
        ConnectionState.Connecting -> stringResource(R.string.connecting)
        ConnectionState.Disconnected -> stringResource(R.string.disconnected)
        is ConnectionState.Error -> stringResource(R.string.connection_error)
    }
    val color = when (state) {
        ConnectionState.Connected -> MaterialTheme.colorScheme.secondary
        ConnectionState.Connecting -> MaterialTheme.colorScheme.primary
        is ConnectionState.Error -> MaterialTheme.colorScheme.error
        ConnectionState.Disconnected -> MaterialTheme.colorScheme.outline
    }
    Row(modifier.background(color.copy(alpha = .14f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 5.dp)) {
        Text(label, color = color, style = MaterialTheme.typography.labelSmall)
    }
}
