package com.magicvector.manager.mine

import com.magicvector.domain.dto.http.response.VideoCloudListResponse
import com.magicvector.domain.dto.http.response.VideoDownloadUrlResponse
import com.magicvector.domain.dto.http.response.VideoPlayUrlResponse
import com.magicvector.MainApplication

class MineVideoManager {
    private val api = MainApplication.getRemoteApiSource()

    suspend fun fetchCloudRecordList(
        userId: String,
        page: Int,
        size: Int
    ): VideoCloudListResponse {
        return api.getCloudVideoList(userId = userId, page = page, size = size)
    }

    suspend fun resolveCloudPlayUrl(videoId: String): VideoPlayUrlResponse {
        return api.getCloudVideoPlayUrl(videoId = videoId)
    }

    suspend fun resolveCloudDownloadUrl(videoId: String): VideoDownloadUrlResponse {
        return api.getCloudVideoDownloadUrl(videoId = videoId)
    }
}
