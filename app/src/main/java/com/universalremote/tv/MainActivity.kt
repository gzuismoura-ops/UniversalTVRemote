package com.universalremote.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.universalremote.tv.navigation.AppNavigation
import com.universalremote.tv.ui.theme.UniversalTVTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TVRemoteApp
        setContent { val dark by app.darkMode.collectAsState(); UniversalTVTheme(darkTheme = dark) { AppNavigation() } }
    }
}
