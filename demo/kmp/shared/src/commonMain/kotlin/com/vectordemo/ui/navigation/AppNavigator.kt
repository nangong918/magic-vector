package com.vectordemo.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppNavigator {
    private val routeStack = MutableStateFlow(listOf(AppRoute.START))
    val routes: StateFlow<List<AppRoute>> = routeStack.asStateFlow()

    val currentRoute: AppRoute
        get() = routeStack.value.lastOrNull() ?: AppRoute.START

    fun resetTo(route: AppRoute) {
        routeStack.value = listOf(route)
    }

    fun navigate(route: AppRoute) {
        routeStack.update { stack ->
            if (stack.lastOrNull() == route) stack else stack + route
        }
    }

    fun goBack(): Boolean {
        val stack = routeStack.value
        if (stack.size <= 1) return false
        routeStack.value = stack.dropLast(1)
        return true
    }
}
