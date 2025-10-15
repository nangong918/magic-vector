package com.data.domain.ao.message

import com.data.domain.vo.message.MessageContactItemVo
import java.io.Serializable

class MessageContactItemAo : Serializable {

    // view
    // 单个联系人Vo信息
    var vo : MessageContactItemVo = MessageContactItemVo()

    // data
    // contactId, 也是agentId
    var contactId: String? = null
    var timestamp: Long = 0L // long 用于排序 不是用于显示时间

    fun setByThat(that : MessageContactItemAo) {
        this.vo.setByThat(that.vo)
        this.contactId = that.contactId
        this.timestamp = that.timestamp
    }
}