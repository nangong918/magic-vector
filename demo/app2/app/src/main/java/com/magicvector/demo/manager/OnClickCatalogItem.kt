package com.magicvector.demo.manager

import com.magicvector.demo.domain.vo.CatalogItem

interface OnClickCatalogItem {
    fun onClick(item: CatalogItem)
}