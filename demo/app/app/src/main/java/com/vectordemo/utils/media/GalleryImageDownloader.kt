package com.vectordemo.utils.media

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * 将网络图片保存到系统相册（Pictures/VectorDemo），便于在「相册」应用中看到。
 * Android 10+ 使用 [MediaStore]；API 28 写入公共目录并触发媒体扫描。
 */
object GalleryImageDownloader {

    fun downloadToGallery(context: Context, imageUrl: String, rawDisplayName: String) {
        val safeName = rawDisplayName
            .replace(Regex("[^a-zA-Z0-9._-]"), "_")
            .ifBlank { "download_${System.currentTimeMillis()}.jpg" }
        val mime = guessMime(safeName)
        URL(imageUrl).openStream().use { input ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                saveWithMediaStore(context, input, safeName, mime)
            } else {
                saveLegacyPublicPictures(context, input, safeName, mime)
            }
        }
    }

    private fun guessMime(name: String): String = when {
        name.endsWith(".png", ignoreCase = true) -> "image/png"
        name.endsWith(".webp", ignoreCase = true) -> "image/webp"
        else -> "image/jpeg"
    }

    private fun saveWithMediaStore(
        context: Context,
        input: java.io.InputStream,
        displayName: String,
        mime: String
    ) {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(
                MediaStore.Images.Media.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/VectorDemo"
            )
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建相册条目")
        resolver.openOutputStream(uri)?.use { output ->
            input.copyTo(output)
        } ?: error("无法写入相册文件")
        values.clear()
        values.put(MediaStore.Images.Media.IS_PENDING, 0)
        resolver.update(uri, values, null, null)
    }

    @Suppress("DEPRECATION")
    private fun saveLegacyPublicPictures(
        context: Context,
        input: java.io.InputStream,
        displayName: String,
        mime: String
    ) {
        val base = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
        val dir = File(base, "VectorDemo").apply { mkdirs() }
        val file = File(dir, displayName)
        FileOutputStream(file).use { out -> input.copyTo(out) }
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mime),
            null
        )
    }
}
