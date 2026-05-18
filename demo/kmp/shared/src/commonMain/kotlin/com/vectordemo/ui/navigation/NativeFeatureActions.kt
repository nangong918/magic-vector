package com.vectordemo.ui.navigation

data class NativeFeatureActions(
    val openLivePush: () -> Boolean = { false },
    val openLivePull: () -> Boolean = { false },
    val openStlCpp: () -> Boolean = { false },
)
