package com.magicvector.manager.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NetworkManager(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val hasRegistered = AtomicBoolean(false)
    private val reconnecting = AtomicBoolean(false)
    @Volatile
    private var wsReconnectAction: (() -> Unit)? = null
    private val _state = MutableStateFlow(
        NetworkState(
            isNetworkOnline = isOnline(),
            isWsConnected = false
        )
    )
    val state: StateFlow<NetworkState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val online = isOnline()
            _state.value = _state.value.copy(isNetworkOnline = online)
            if (online && !_state.value.isWsConnected) {
                triggerWsReconnect()
            }
        }
    }

    fun register() {
        if (!hasRegistered.compareAndSet(false, true)) {
            return
        }
        val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            appContext.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            appContext.registerReceiver(receiver, filter)
        }
    }

    fun unregister() {
        if (hasRegistered.compareAndSet(true, false)) {
            runCatching { appContext.unregisterReceiver(receiver) }
        }
    }

    fun onWebSocketConnected() {
        reconnecting.set(false)
        _state.value = _state.value.copy(isWsConnected = true)
    }

    fun onWebSocketDisconnected() {
        _state.value = _state.value.copy(isWsConnected = false)
        if (_state.value.isNetworkOnline) {
            triggerWsReconnect()
        }
    }

    fun bindWsReconnectAction(action: () -> Unit) {
        wsReconnectAction = action
    }

    fun unbindWsReconnectAction() {
        wsReconnectAction = null
    }

    private fun triggerWsReconnect() {
        if (!reconnecting.compareAndSet(false, true)) {
            return
        }
        val action = wsReconnectAction
        if (action == null) {
            reconnecting.set(false)
            return
        }
        runCatching {
            action.invoke()
        }.onFailure {
            reconnecting.set(false)
        }
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

data class NetworkState(
    val isNetworkOnline: Boolean,
    val isWsConnected: Boolean
) {
    val isOnlineAndWsReady: Boolean
        get() = isNetworkOnline && isWsConnected
}
