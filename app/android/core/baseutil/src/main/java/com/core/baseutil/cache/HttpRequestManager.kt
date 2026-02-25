package com.core.baseutil.cache

import android.text.TextUtils
import java.util.concurrent.ConcurrentHashMap

object HttpRequestManager {

    // 线程安全的Map<String, Boolean> 类似Redis
    val isFirstOpenMap: ConcurrentHashMap<String, Boolean> = ConcurrentHashMap<String, Boolean>(HashMap<String, Boolean>())

    private fun setIsFirstOpenValue(key: String?) {
        if (TextUtils.isEmpty(key)) {
            return
        }
        isFirstOpenMap.put(key!!, false)
    }

    // 判断某个key的变量是否第一次打开
    fun getIsFirstOpen(key: String?): Boolean {
        if (TextUtils.isEmpty(key)) {
            return false
        }
        // 如果没有Key就是第一次打开
        if (isFirstOpenMap[key] == null) {
            setIsFirstOpenValue(key)
            return true
        }
        return true == isFirstOpenMap[key]
    }

    // 刷新全部值 (在网络断开的时候调用)
    fun refreshAllValue() {
        isFirstOpenMap.clear()
    }
}