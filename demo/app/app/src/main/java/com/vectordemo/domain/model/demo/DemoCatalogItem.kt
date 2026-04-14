package com.vectordemo.domain.model.demo

data class DemoCatalogItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val route: DemoRoute
)

enum class DemoRoute {
    HELLO,
    OSS_DEMO
}
