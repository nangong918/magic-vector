package com.magicvector.demo.domain.vo

data class CatalogItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconRes: Int? = null,
    val cls: Class<*>
)
