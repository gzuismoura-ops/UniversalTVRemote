package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R
import com.universalremote.tv.model.DiscoveredTV

@Composable
fun TVCard(tv: DiscoveredTV, saved: Boolean, onConnect: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(tv.brand.emoji, style = MaterialTheme.typography.headlineMedium)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(tv.friendlyName, style = MaterialTheme.typography.titleLarge)
                Text("${tv.brand.displayName} · ${tv.modelName.ifBlank { stringResource(R.string.model_unknown) }}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${tv.ip} · ${tv.protocol}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (saved) Text(stringResource(R.string.previously_saved), color = MaterialTheme.colorScheme.secondary, style = MaterialTheme.typography.labelSmall)
            }
            RemoteButton(stringResource(if (tv.requiresPairing) R.string.pair else R.string.connect), onConnect)
        }
    }
}
