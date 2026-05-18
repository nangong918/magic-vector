package com.vectordemo.domain.model

data class UserSessionModel(
    val userId: Long,
    val account: String,
    val name: String,
    val avatarUrl: String = "",
    val accessToken: String,
    val password: String = "",
    val isCurrent: Boolean = false,
    val lastLoginAt: Long = 0L,
)

data class OssUserBucketListModel(
    val userId: Long,
    val bucketNames: List<String>,
)

data class OssBucketFileItemModel(
    val fileId: Long,
    val originFileName: String,
    val url: String,
)

data class OssBucketFileItemListModel(
    val userId: Long,
    val bucketName: String,
    val items: List<OssBucketFileItemModel>,
)

data class DemoCatalogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val route: DemoRoute,
)

enum class DemoRoute {
    HELLO,
    OSS_DEMO,
    CHAT_LIST_DEMO,
    VOICE_AGENT,
    LIVE_PUSH,
    LIVE_PULL,
    STL_CPP,
}
