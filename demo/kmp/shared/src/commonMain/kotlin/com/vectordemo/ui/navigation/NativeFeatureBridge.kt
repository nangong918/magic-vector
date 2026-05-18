package com.vectordemo.ui.navigation

object NativeFeatureBridge {
    @Volatile
    private var actions: NativeFeatureActions = NativeFeatureActions()

    fun setActions(value: NativeFeatureActions) {
        actions = value
    }

    fun openLivePush(): Boolean = actions.openLivePush()

    fun openLivePull(): Boolean = actions.openLivePull()

    fun openStlCpp(): Boolean = actions.openStlCpp()
}
