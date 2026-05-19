package com.vectordemo.ui.image

import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

actual fun initCoilPlatform(context: Any?) {
    SingletonImageLoader.setSafe {
        ImageLoader.Builder(it)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }
}
