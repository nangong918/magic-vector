package com.vectordemo.viewModel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

open class BaseVm {
    protected val vmScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
}
