package com.vectordemo.domain.config

import kmp.shared.generated.resources.Res

actual suspend fun readModuleKeyJson(): String {
    return Res.readBytes("files/module_key.json").decodeToString()
}
