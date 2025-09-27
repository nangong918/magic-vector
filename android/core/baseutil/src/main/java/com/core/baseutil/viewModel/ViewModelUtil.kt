package com.core.baseutil.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

object ViewModelUtil {

    private val tag: String = ViewModel::class.java.simpleName

    fun <T : ViewModel> newViewModel(owner: ViewModelStoreOwner, modelClass: Class<T>): T {
        return ViewModelProvider(owner)[modelClass]
    }

    fun <T : ViewModel> newViewModel(owner: ViewModelStoreOwner, factory: ViewModelProvider.Factory, modelClass: Class<T>): T {
        return ViewModelProvider(owner, factory)[modelClass]
    }

}