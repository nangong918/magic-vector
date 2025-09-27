package com.data.domain.constant.ao

import com.data.domain.constant.vo.MessageContactItemVo

class MessageContactItemAo {

    // view
    // 单个联系人Vo信息
    var vo : MessageContactItemVo = MessageContactItemVo()

    // data
    var contactId: Long? = null
    var timestamp: Long = 0L // long 用于排序 不是用于显示时间

    fun setByThat(that : MessageContactItemAo) {
        this.vo.setByThat(that.vo)
        this.contactId = that.contactId
        this.timestamp = that.timestamp
    }
}