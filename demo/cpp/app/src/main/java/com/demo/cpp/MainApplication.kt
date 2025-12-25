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

    /*
        我现在因为从Android已经获取了Bitmap，我不想在用Camera获取流推送后端了。
        为什么获取的是bitmap，因为yoloV8的模型入参是byteArray，所以我就把bitmap输入进去交给它。
        如果是ibp帧的话，单帧需要根据上下帧推理，这一点对于yolo预测模型很不友好，也没法实现。
        所以使用bitmap给它。但是我现在都有bitmap了。
        我不想再次打开camera再次获取当前camera流推流，这样不仅没有复用bitmap，而且还开启了新的camera线程吃性能。
        你现在看看能不能写个方案，把实时bitmap用ffmpeg处理然后交给rtmp高效传递给后端。
        此外还需要用OpenGL高效渲染Bitmap。
     */
}
