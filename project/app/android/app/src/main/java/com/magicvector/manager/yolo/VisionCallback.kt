package com.magicvector.manager.yolo

import android.graphics.Bitmap

interface VisionCallback {
    fun onReceiveCurrentFrameBitmap(bitmap: Bitmap)
}