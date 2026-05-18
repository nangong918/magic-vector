package com.vectordemo.domain.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private var applicationContext: Context? = null

fun initModuleKeyConfigContext(context: Context) {
    applicationContext = context.applicationContext
}

actual suspend fun readModuleKeyJson(): String = withContext(Dispatchers.IO) {
    val context = applicationContext
        ?: error("ModuleKey 未初始化：请在 Application/Activity 中调用 initModuleKeyConfigContext")
    context.assets.open("module_key.json").bufferedReader().use { it.readText() }
}
