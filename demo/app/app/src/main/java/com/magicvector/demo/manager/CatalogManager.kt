package com.magicvector.demo.manager

import android.content.Context
import android.content.Intent
import com.magicvector.demo.activity.ListActivity
import com.magicvector.demo.activity.NetworkActivity
import com.magicvector.demo.domain.vo.CatalogItem

object CatalogManager {

    fun getCatalogItems(): List<CatalogItem> {
        return listOf(
            CatalogItem(
                id ="1",
                title = "网络",
                subtitle = "网络相关Demo",
                cls = NetworkActivity::class.java
            ),
            CatalogItem(
                id ="2",
                title = "My列表",
                subtitle = "自定义compose列表相关Demo",
                cls = ListActivity::class.java
            ),
        )
    }

    fun onItemClick(item: CatalogItem, activity: Context): OnClickCatalogItem{
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