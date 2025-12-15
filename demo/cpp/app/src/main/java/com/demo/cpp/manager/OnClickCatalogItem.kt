package com.demo.cpp.manager

import com.demo.cpp.domain.vo.CatalogItem


interface OnClickCatalogItem {
    fun onClick(item: CatalogItem)
}