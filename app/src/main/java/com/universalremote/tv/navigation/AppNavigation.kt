package com.universalremote.tv.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.ui.screens.PairScreen
import com.universalremote.tv.ui.screens.RemoteScreen
import com.universalremote.tv.ui.screens.ScanScreen
import com.universalremote.tv.ui.screens.SettingsScreen

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as TVRemoteApp
    NavHost(navController = nav, startDestination = "scan") {
        composable("scan") {
            ScanScreen(
                onSelect = { tv ->
                    app.selectedTv = tv
                    nav.navigate(if (tv.requiresPairing) "pair" else "remote")
                },
                onSettings = { nav.navigate("settings") }
            )
        }
        composable("pair") {
            PairScreen(onPaired = { nav.navigate("remote") { popUpTo("pair") { inclusive = true } } }, onBack = { nav.popBackStack() })
        }
        composable("remote") {
            RemoteScreen(onSettings = { nav.navigate("settings") }, onSwitchTV = { nav.navigate("scan") { popUpTo("remote") { inclusive = true } } })
        }
        composable("settings") { SettingsScreen(onBack = { nav.popBackStack() }) }
    }
}
