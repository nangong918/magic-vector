package com.demo.cpp

import android.app.Application

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        var instance: MainApplication? = null
            private set
    }
}
