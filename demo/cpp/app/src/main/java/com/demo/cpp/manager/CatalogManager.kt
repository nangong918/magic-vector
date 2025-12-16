package com.demo.cpp.manager

import android.content.Context
import android.content.Intent
import com.demo.cpp.FFmpegActivity
import com.demo.cpp.OpenGLActivity
import com.demo.cpp.STLActivity
import com.demo.cpp.domain.vo.CatalogItem

object CatalogManager {

    fun getCatalogItems(): List<CatalogItem> {
        return listOf(
            CatalogItem(
                id = "1",
                title = "STL",
                subtitle = "cpp 数据结构 STL Demo",
                cls = STLActivity::class.java
            ),
            CatalogItem(
                id = "2",
                title = "FFmpeg",
                subtitle = "FFmpeg 视频流编解码处理 Demo",
                cls = FFmpegActivity::class.java
            ),
            CatalogItem(
                id = "3",
                title = "OpenGL",
                subtitle = "OpenGL视频帧GPU加速绘制 Demo",
                cls = OpenGLActivity::class.java
            ),
        )
    }

    fun onItemClick(item: CatalogItem, activity: Context): OnClickCatalogItem {
        val intent = Intent(
            activity,
            item.cls
        )
        return object : OnClickCatalogItem {
            override fun onClick(item: CatalogItem) {
                activity.startActivity(intent)
            }
        }
    }

}