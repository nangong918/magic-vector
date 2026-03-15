package com.magicvector.manager.mine

import com.core.baseutil.network.BaseResponse
import com.core.baseutil.network.OnSuccessCallback
import com.core.baseutil.network.OnThrowableCallback
import com.magicvector.domain.dto.http.response.VideoCloudListResponse
import com.magicvector.domain.dto.http.response.VideoDownloadUrlResponse
import com.magicvector.domain.dto.http.response.VideoPlayUrlResponse
import com.magicvector.MainApplication

class MineVideoManager {
    private val api = MainApplication.getApiRequestImplInstance()

    fun fetchCloudRecordList(
        userId: String,
        page: Int,
        size: Int,
        onSuccess: (VideoCloudListResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        api.getCloudVideoList(
            userId = userId,
            page = page,
            size = size,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<VideoCloudListResponse>> {
                override fun onResponse(response: BaseResponse<VideoCloudListResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun resolveCloudPlayUrl(
        videoId: String,
        onSuccess: (VideoPlayUrlResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        api.getCloudVideoPlayUrl(
            videoId = videoId,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<VideoPlayUrlResponse>> {
                override fun onResponse(response: BaseResponse<VideoPlayUrlResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }

    fun resolveCloudDownloadUrl(
        videoId: String,
        onSuccess: (VideoDownloadUrlResponse?) -> Unit,
        onError: (Throwable?) -> Unit
    ) {
        api.getCloudVideoDownloadUrl(
            videoId = videoId,
            onSuccessCallback = object : OnSuccessCallback<BaseResponse<VideoDownloadUrlResponse>> {
                override fun onResponse(response: BaseResponse<VideoDownloadUrlResponse>?) {
                    onSuccess.invoke(response?.data)
                }
            },
            throwableCallback = object : OnThrowableCallback {
                override fun callback(throwable: Throwable?) {
                    onError.invoke(throwable)
                }
            }
        )
    }
}
