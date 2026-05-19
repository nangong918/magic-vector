package com.vectordemo.ui.image

import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

actual fun initCoilPlatform(context: Any?) {
    val appContext = (context as? Context)?.applicationContext ?: return
    SingletonImageLoader.setSafe { ctx ->
        ImageLoader.Builder(ctx)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .crossfade(true)
            .build()
    }
}
