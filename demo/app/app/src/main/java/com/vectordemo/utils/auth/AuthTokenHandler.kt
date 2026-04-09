package com.vectordemo.utils.auth

import android.content.Context
import android.content.Intent
import com.vectordemo.MainApplication
import com.vectordemo.activity.LoginActivity
import com.vectordemo.domain.exception.UserExceptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AuthTokenHandler {
    fun handleTokenExpired(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            MainApplication.getUserManager().clearCurrentUser()
            MainApplication.clearUserId()
            context.startActivity(Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }

    fun isTokenExpiredCode(code: String): Boolean {
        return code == UserExceptions.ACCESS_TOKEN_INVALID.code || code == UserExceptions.NO_TOKEN_FORBIDDEN_CALL_API.code
    }
}
