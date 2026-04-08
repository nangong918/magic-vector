package com.magicvector.domain.constant

enum class MainSelectEnum(val position: Int) {
    AGENT(0),
    CONTROL(1),
    MINE(2);

    companion object {
        fun getItem(position: Int): MainSelectEnum? {
            return MainSelectEnum.entries.find { it.position == position }
        }
    }
}