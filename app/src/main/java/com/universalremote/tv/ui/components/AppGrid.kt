package com.universalremote.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.universalremote.tv.R
import com.universalremote.tv.model.TVApp

@Composable
fun AppGrid(apps: List<TVApp>, onLaunch: (String) -> Unit, onAllApps: () -> Unit, modifier: Modifier = Modifier) {
    val visible = apps.ifEmpty {
        listOf(
            TVApp("Netflix", stringResource(R.string.app_netflix)),
            TVApp("YouTube", stringResource(R.string.app_youtube)),
            TVApp("PrimeVideo", stringResource(R.string.app_prime_video)),
            TVApp("DisneyPlus", stringResource(R.string.app_disney_plus)),
            TVApp("Spotify", stringResource(R.string.app_spotify)),
            TVApp("Plex", stringResource(R.string.app_plex))
        )
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        visible.chunked(2).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { app -> RemoteButton(app.name, { onLaunch(app.id) }, Modifier.weight(1f)) }
                if (row.size == 1) androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
            }
        }
        RemoteButton(stringResource(R.string.all_apps), onAllApps, Modifier.fillMaxWidth())
    }
}
