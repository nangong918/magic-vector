package com.vectordemo.ui.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object AppNavigator {
    private val routeStack = MutableStateFlow(listOf(AppRoute.START))
    val routes: StateFlow<List<AppRoute>> = routeStack.asStateFlow()

    var stackListener: ((List<AppRoute>) -> Unit)? = null

    val currentRoute: AppRoute
        get() = routeStack.value.lastOrNull() ?: AppRoute.START

    fun restore(stack: List<AppRoute>) {
        if (stack.isNotEmpty()) {
            publishStack(stack)
        }
    }

    fun resetTo(route: AppRoute) {
        publishStack(listOf(route))
    }

    fun navigate(route: AppRoute) {
        val stack = routeStack.value
        val next = if (stack.lastOrNull() == route) stack else stack + route
        publishStack(next)
    }

    fun goBack(): Boolean {
        val stack = routeStack.value
        if (stack.size <= 1) return false
        publishStack(stack.dropLast(1))
        return true
    }

    private fun publishStack(stack: List<AppRoute>) {
        routeStack.value = stack
        stackListener?.invoke(stack)
    }
}
