package com.universalremote.tv

import android.app.Application
import com.universalremote.tv.data.CredentialStore
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.network.NetworkScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TVRemoteApp : Application() {
    lateinit var credentialStore: CredentialStore
        private set
    lateinit var networkScanner: NetworkScanner
        private set
    @Volatile var selectedTv: DiscoveredTV? = null
    private val preferences by lazy { getSharedPreferences("app_settings", MODE_PRIVATE) }
    private val mutableDarkMode = MutableStateFlow(true)
    val darkMode = mutableDarkMode.asStateFlow()
    var wakeOnLanEnabled: Boolean
        get() = preferences.getBoolean("wol_enabled", true)
        set(value) { preferences.edit().putBoolean("wol_enabled", value).apply() }

    fun setDarkMode(enabled: Boolean) { preferences.edit().putBoolean("dark_mode", enabled).apply(); mutableDarkMode.value = enabled }

    override fun onCreate() {
        super.onCreate()
        credentialStore = CredentialStore(this)
        networkScanner = NetworkScanner(this)
        mutableDarkMode.value = preferences.getBoolean("dark_mode", true)
    }
}
