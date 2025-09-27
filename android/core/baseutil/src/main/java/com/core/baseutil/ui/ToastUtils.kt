package com.core.baseutil.ui

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast

object ToastUtils {

    val TAG: String = ToastUtils::class.java.name

    /**
     * 显示Toast消息
     * @param context   上下文
     * @param message   消息内容
     */
    fun showToastActivity(context: Context?, message: String?) {
        if (context == null) {
            return
        }
        if (context is Activity) {
            context.runOnUiThread {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
        else {
            try {
                val handler = Handler(Looper.getMainLooper())
                handler.post {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            } catch (e : Exception){
                Log.e(TAG, "showToast: ", e)
            }
        }
    }

}