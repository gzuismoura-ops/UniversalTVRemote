package com.universalremote.tv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.controller.BaseTVController
import com.universalremote.tv.controller.TVControllerFactory
import com.universalremote.tv.model.ConnectionState
import com.universalremote.tv.model.DiscoveredTV
import com.universalremote.tv.model.TVApp
import com.universalremote.tv.model.TVKey
import com.universalremote.tv.network.WakeOnLan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RemoteViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TVRemoteApp
    private var controller: BaseTVController? = null
    private val mutableConnection = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connection: StateFlow<ConnectionState> = mutableConnection.asStateFlow()
    private val mutableError = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = mutableError.asStateFlow()
    private val mutableApps = MutableStateFlow<List<TVApp>>(emptyList())
    val apps: StateFlow<List<TVApp>> = mutableApps.asStateFlow()
    val tv: DiscoveredTV? get() = app.selectedTv

    fun connect() {
        val device = tv ?: run { mutableConnection.value = ConnectionState.Error("No TV selected"); return }
        mutableConnection.value = ConnectionState.Connecting
        viewModelScope.launch {
            val next = TVControllerFactory.create(device, app.credentialStore)
            val result = next.connect()
            result.fold({ controller?.disconnect(); controller = next; app.credentialStore.saveTV(device); mutableConnection.value = ConnectionState.Connected }, {
                controller = next
                mutableConnection.value = ConnectionState.Error(it.message ?: "Connection failed")
                mutableError.value = it.message
            })
        }
    }

    fun pair(pin: String?) {
        val device = tv ?: return
        mutableConnection.value = ConnectionState.Connecting
        viewModelScope.launch {
            val next = controller ?: TVControllerFactory.create(device, app.credentialStore).also { controller = it }
            val result = next.pair(pin)
            result.fold({ mutableConnection.value = ConnectionState.Connected }, { mutableConnection.value = ConnectionState.Error(it.message ?: "Pairing failed"); mutableError.value = it.message })
        }
    }

    fun send(key: TVKey) {
        viewModelScope.launch {
            val active = controller ?: run { mutableError.value = "Connect to a TV first"; return@launch }
            val result = active.sendKey(key)
            result.exceptionOrNull()?.let { mutableError.value = it.message ?: "Command failed" }
        }
    }

    fun launchApp(id: String) { viewModelScope.launch {
        val result = controller?.launchApp(id) ?: Result.failure(IllegalStateException("Connect to a TV first"))
        result.exceptionOrNull()?.let { mutableError.value = it.message ?: "Could not launch app" }
    } }

    fun loadApps() { viewModelScope.launch {
        val result = controller?.getAppList() ?: Result.failure(IllegalStateException("Connect to a TV first"))
        result.fold({ mutableApps.value = it }, { mutableError.value = it.message ?: "Could not load apps" })
    } }

    fun toggleMute(mute: Boolean) { viewModelScope.launch {
        val result = controller?.setMute(mute) ?: Result.failure(IllegalStateException("Connect to a TV first"))
        result.exceptionOrNull()?.let { mutableError.value = it.message ?: "Mute failed" }
    } }

    fun powerOff() { command { it.powerOff() } }
    fun sendVolumeUp() { command { it.volumeUp() } }
    fun sendVolumeDown() { command { it.volumeDown() } }
    fun sendChannelUp() { command { it.channelUp() } }
    fun sendChannelDown() { command { it.channelDown() } }

    private fun command(action: suspend (BaseTVController) -> Result<Unit>) { viewModelScope.launch {
        val result = controller?.let { action(it) } ?: Result.failure(IllegalStateException("Connect to a TV first"))
        result.exceptionOrNull()?.let { mutableError.value = it.message ?: "Command failed" }
    } }

    fun wakeUp() { viewModelScope.launch {
        val device = tv ?: return@launch
        val mac = device.macAddress ?: app.credentialStore.getCredential(device.ip, "macAddress")
        if (mac.isNullOrBlank()) mutableError.value = "No MAC address is known for this TV. WOL cannot be sent until it is saved."
        else WakeOnLan.send(mac).exceptionOrNull()?.let { mutableError.value = it.message ?: "Wake-on-LAN failed" }
    } }

    fun clearError() { mutableError.value = null }
    override fun onCleared() { controller?.let { c -> viewModelScope.launch { c.disconnect() } } }
}
