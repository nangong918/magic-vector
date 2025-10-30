package com.magicvector.manager.mcp

import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayOutputStream

object VisionMcpManager {

    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 将 Bitmap 压缩为 JPEG 格式，并写入 ByteArrayOutputStream
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        // 将字节数组编码为 Base64 字符串
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

}