package com.view.appview

enum class MainSelectItemEnum(val position: Int) {
    HOME(0),
    APPLY(1),
    MINE(2);

    companion object {
        const val INTENT_EXTRA_NAME = "MainBottomBar.SelectItem"

        fun getItem(position: Int): MainSelectItemEnum? {
            return MainSelectItemEnum.entries.find { it.position == position }
        }
    }
}