package com.vectordemo.domain.config

expect suspend fun readModuleKeyJson(): String

object ModuleKeyConfigLoader {
    suspend fun loadContent(): String = readModuleKeyJson()
}
