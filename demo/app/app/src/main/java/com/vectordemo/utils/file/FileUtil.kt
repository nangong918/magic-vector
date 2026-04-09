package com.vectordemo.utils.file

import android.content.ContentResolver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

object FileUtil {
    fun createMultipartBodyPart(file: File, fileRequestParamName: String): MultipartBody.Part {
        return MultipartBody.Part.createFormData(
            fileRequestParamName,
            file.name,
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        )
    }

    fun getFilePathFromContentUri(uri: Uri, resolver: ContentResolver): String? {
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor: Cursor? = resolver.query(uri, projection, null, null, null)
        cursor ?: return null
        cursor.use {
            if (!it.moveToFirst()) return null
            val index = it.getColumnIndexOrThrow(projection[0])
            return it.getString(index)
        }
    }
}
