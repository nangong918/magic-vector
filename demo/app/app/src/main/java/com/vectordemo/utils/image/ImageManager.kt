package com.vectordemo.utils.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.vectordemo.utils.file.FileUtil
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class ImageManager {
    fun uriToBitmapMediaStore(context: Context, uri: Uri): Bitmap? {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return BitmapFactory.decodeStream(input)
        }
        return null
    }

    fun processImage(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxSize && height <= maxSize) return bitmap
        val scale = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        val newW = (width * scale).toInt()
        val newH = (height * scale).toInt()
        return Bitmap.createScaledBitmap(bitmap, newW, newH, true)
    }

    fun bitmapToFile(bitmap: Bitmap, uri: Uri, context: Context): File? {
        val path = FileUtil.getFilePathFromContentUri(uri, context.contentResolver) ?: return null
        val file = File(path)
        ByteArrayOutputStream().use { bos ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bos)
            FileOutputStream(file).use { it.write(bos.toByteArray()) }
        }
        return file
    }
}
