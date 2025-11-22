package com.magicvector.demo.domain.vo

data class SelectItem(
    val id: String,
    val title: String,
    val subtitle: String? = null,
    val iconRes: Int? = null
)
