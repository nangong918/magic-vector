package com.vectordemo

import android.app.Application
import com.vectordemo.dataSource.remote.RemoteApiSource
import com.vectordemo.manager.user.UserManager
import com.vectordemo.repository.api.ApiRequest
import com.vectordemo.repository.api.config.ApiRequestProvider
import com.vectordemo.utils.image.ImageManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        app = this
        apiRequestInstance = ApiRequestProvider.getApiRequest()
    }

    companion object {
        private lateinit var app: MainApplication
        private var apiRequestInstance: ApiRequest? = null
        private var remoteApiSource: RemoteApiSource? = null
        private var userManager: UserManager? = null
        private var imageManager: ImageManager? = null
        @Volatile
        private var cachedUserId: String = ""

        fun getApp(): MainApplication = app

        fun getRemoteApiSource(): RemoteApiSource {
            if (remoteApiSource == null) remoteApiSource = RemoteApiSource(apiRequestInstance!!)
            return remoteApiSource!!
        }

        fun getUserManager(): UserManager {
            if (userManager == null) userManager = UserManager.getInstance(getApp())
            return userManager!!
        }

        fun getImageManager(): ImageManager? {
            if (imageManager == null) imageManager = ImageManager()
            return imageManager
        }

        fun getUserId(): String = cachedUserId
        fun updateUserId(userId: Long) {
            cachedUserId = if (userId > 0) userId.toString() else ""
        }
        fun clearUserId() {
            cachedUserId = ""
        }
    }
}
