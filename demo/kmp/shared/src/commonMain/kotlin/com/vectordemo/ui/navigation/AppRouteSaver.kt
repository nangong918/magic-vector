package com.vectordemo.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue

private fun encodeRouteStack(stack: List<AppRoute>): String =
    stack.joinToString(separator = ",") { it.name }

private fun decodeRouteStack(raw: String): List<AppRoute> {
    if (raw.isBlank()) return listOf(AppRoute.START)
    return raw.split(',').filter { it.isNotBlank() }.map { AppRoute.valueOf(it) }
}

@Composable
fun rememberSyncedAppRoutes(): List<AppRoute> {
    var savedStackKey by rememberSaveable {
        mutableStateOf(encodeRouteStack(listOf(AppRoute.START)))
    }
    val restoredRoutes = decodeRouteStack(savedStackKey)

    DisposableEffect(savedStackKey) {
        AppNavigator.restore(restoredRoutes)
        AppNavigator.stackListener = { stack ->
            savedStackKey = encodeRouteStack(stack)
        }
        onDispose {
            AppNavigator.stackListener = null
        }
    }

    return restoredRoutes
}
