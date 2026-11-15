package com.universalremote.tv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.model.DiscoveredTV
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScanUiState {
    data object Idle : ScanUiState
    data class Scanning(val message: String, val found: List<DiscoveredTV>) : ScanUiState
    data class Ready(val devices: List<DiscoveredTV>) : ScanUiState
    data class Error(val message: String) : ScanUiState
}

class ScanViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TVRemoteApp
    private val mutableState = MutableStateFlow<ScanUiState>(ScanUiState.Idle)
    val state: StateFlow<ScanUiState> = mutableState.asStateFlow()
    private var scanJobRunning = false

    fun scan() {
        if (scanJobRunning) return
        scanJobRunning = true
        mutableState.value = ScanUiState.Scanning("Preparing network scan", emptyList())
        viewModelScope.launch {
            try {
                val devices = app.networkScanner.scan { message ->
                    val current = mutableState.value
                    val found = if (current is ScanUiState.Scanning) current.found else emptyList()
                    mutableState.value = ScanUiState.Scanning(message, found)
                }
                devices.forEach(app.credentialStore::saveTV)
                mutableState.value = ScanUiState.Ready(devices)
            } catch (e: Exception) {
                mutableState.value = ScanUiState.Error(e.message?.takeIf { it.isNotBlank() } ?: "Network scan failed. Check Wi-Fi and permissions.")
            } finally {
                scanJobRunning = false
            }
        }
    }

    fun savedDevices(): List<DiscoveredTV> = app.credentialStore.getAllSavedTVs()
    fun select(tv: DiscoveredTV) { app.selectedTv = tv }
}
