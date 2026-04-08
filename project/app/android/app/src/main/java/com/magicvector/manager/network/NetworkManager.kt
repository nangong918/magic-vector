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
            val latestState = refreshNetworkState()
            // 只有在有网络的情况下才能执行ws断开重连
            if (latestState.isNetworkOnline && !latestState.isWsConnected) {
                requestWsReconnectIfNeeded()
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

    fun onWebSocketDisconnected(shouldReconnect: Boolean = true) {
        _state.value = _state.value.copy(isWsConnected = false)
        if (shouldReconnect) {
            requestWsReconnectIfNeeded()
        } else {
            reconnecting.set(false)
        }
    }

    fun bindWsReconnectAction(action: () -> Unit) {
        wsReconnectAction = action
    }

    fun unbindWsReconnectAction() {
        wsReconnectAction = null
        reconnecting.set(false)
    }

    fun refreshNetworkState(): NetworkState {
        val latest = updateNetworkOnlineState(isOnline())
        return latest
    }

    fun requestWsReconnectIfNeeded() {
        val latest = refreshNetworkState()
        if (!latest.isNetworkOnline || latest.isWsConnected) {
            reconnecting.set(false)
            return
        }
        triggerWsReconnect()
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

    private fun updateNetworkOnlineState(online: Boolean): NetworkState {
        val current = _state.value
        val nextRecoveryToken = if (!current.isNetworkOnline && online) {
            current.recoveryToken + 1
        } else {
            current.recoveryToken
        }
        val latest = current.copy(
            isNetworkOnline = online,
            recoveryToken = nextRecoveryToken
        )
        _state.value = latest
        return latest
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}

data class NetworkState(
    val isNetworkOnline: Boolean,
    val isWsConnected: Boolean,
    val recoveryToken: Long = 0L
) {
    val isOnlineAndWsReady: Boolean
        get() = isNetworkOnline && isWsConnected
}
