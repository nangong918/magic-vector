package com.view.appview.recycler

enum class UpdateRecyclerViewTypeEnum(i: Int) {
    ID_TO_END_UPDATE(0),
    SINGLE_ID_UPDATE(1),

//    POSITION_TO_END_UPDATE(2),
//    SINGLE_POSITION_UPDATE(3),
//    RANGE_UPDATE(4),
//    ID_LIST_UPDATE(5),
//    POSITION_LIST_UPDATE(6),
//    ALL_UPDATE(7),
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