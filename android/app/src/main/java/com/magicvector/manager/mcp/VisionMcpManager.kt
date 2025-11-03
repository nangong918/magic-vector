package com.magicvector.manager.mcp

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object VisionMcpManager {

    // 将 Bitmap 转换为 Base64
    fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        // 将 Bitmap 压缩为 JPEG 格式，并写入 ByteArrayOutputStream
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        // 将字节数组编码为 Base64 字符串
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }

    // 将Bitmap转为file
    fun bitmapToFile(bitmap: Bitmap, context: Context): File {
        val fileName = "image_${System.currentTimeMillis()}.jpg"

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Android 10+ 使用应用专属目录
            val file = File(context.filesDir, "images/$fileName")
            file.parentFile?.mkdirs() // 创建目录

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            file
        } else {
            // Android 10 以下使用传统方式
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val file = File(storageDir, fileName)

            FileOutputStream(file).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos)
            }
            file
        }
    }

}