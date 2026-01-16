package com.demo.cpp.manager

import android.content.Context
import android.content.Intent
import com.demo.cpp.activity.CameraFilterActivity
import com.demo.cpp.STLActivity
import com.demo.cpp.activity.LiveActivity
import com.demo.cpp.activity.PushActivity
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
                title = "CameraFilter",
                subtitle = "OpenGL 实现的Camera滤镜 Demo",
                cls = CameraFilterActivity::class.java
            ),
            CatalogItem(
                id = "3",
                title = "Live",
                subtitle = "RTMP 直播推流 Demo",
                cls = LiveActivity::class.java
            ),
            CatalogItem(
                id = "4",
                title = "Push",
                subtitle = "FFmpeg 推流 Demo",
                cls = PushActivity::class.java
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