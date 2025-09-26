package com.magicvector

import android.app.Application

class MainApplication : Application() {

    val tag = MainApplication::class.simpleName

    lateinit var mApp: MainApplication

    //----------------------------启动APP调用----------------------------

    override fun onCreate() {
        super.onCreate()
        mApp = this
        initGlobal()
    }

    //----------------------------global----------------------------

    private fun initGlobal() {
    }

    //----------------------------utils----------------------------


    //----------------------------APP终止的时候调用----------------------------
    override fun onTerminate() {
        super.onTerminate()
    }
}