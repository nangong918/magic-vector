package com.vectordemo.domain.config

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private var applicationContext: Context? = null

fun initModuleKeyConfigContext(context: Context) {
    applicationContext = context.applicationContext
}

internal fun requireApplicationContext(): Context =
    applicationContext ?: error("Android Context 未初始化：请在 Activity 中调用 initModuleKeyConfigContext")

actual suspend fun readModuleKeyJson(): String = withContext(Dispatchers.IO) {
    val context = requireApplicationContext()
    context.assets.open("module_key.json").bufferedReader().use { it.readText() }
}
