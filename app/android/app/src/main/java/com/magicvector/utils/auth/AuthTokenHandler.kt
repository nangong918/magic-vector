package com.magicvector.utils.auth

import android.content.Context
import com.core.baseutil.fragmentActivity.ActivityLaunchUtils
import com.magicvector.MainApplication
import com.magicvector.activity.ComposeLoginActivity
import com.magicvector.domain.exception.UserExceptions
import com.magicvector.manager.user.UserManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object AuthTokenHandler {

    fun handleTokenExpired(context: Context) {
        CoroutineScope(Dispatchers.Main).launch {
            clearUserSession()
            navigateToLogin(context)
        }
    }

    fun isTokenExpiredCode(code: String): Boolean {
        return code == UserExceptions.ACCESS_TOKEN_INVALID.code || code == UserExceptions.NO_TOKEN_FORBIDDEN_CALL_API.code
    }

    private suspend fun clearUserSession() {
        val userManager: UserManager = MainApplication.getUserManager()
        userManager.clearCurrentUser()
        MainApplication.clearUserId()
    }

    private fun navigateToLogin(context: Context) {
        ActivityLaunchUtils.launchNewTask(
            context = context,
            activityClass = ComposeLoginActivity::class.java,
            config = null
        )
    }
}
