package com.view.appview.recycler

enum class UpdateRecyclerViewTypeEnum(i: Int) {
    ID_TO_END_UPDATE(0),
    SINGLE_ID_UPDATE(1),
    SINGLE_ID_INSERT(2),
    ;

    fun getType(): Int {
        return this.ordinal
    }

    companion object {
        fun getType(type: Int): UpdateRecyclerViewTypeEnum {
            return entries[type]
        }
    }

}