package com.universalremote.tv.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.universalremote.tv.TVRemoteApp
import com.universalremote.tv.controller.BaseTVController
import com.universalremote.tv.controller.TVControllerFactory
import com.universalremote.tv.model.ConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PairViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TVRemoteApp
    private val mutableState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    private var controller: BaseTVController? = null
    val state: StateFlow<ConnectionState> = mutableState.asStateFlow()

    fun pair(pin: String?) {
        val tv = app.selectedTv ?: run { mutableState.value = ConnectionState.Error("No TV selected"); return }
        mutableState.value = ConnectionState.Connecting
        viewModelScope.launch {
            val active = controller ?: TVControllerFactory.create(tv, app.credentialStore).also { controller = it }
            val result = active.pair(pin)
            mutableState.value = result.fold({ ConnectionState.Connected }, { ConnectionState.Error(it.message ?: "Pairing failed") })
            if (result.isSuccess) app.credentialStore.saveTV(tv)
        }
    }
}
