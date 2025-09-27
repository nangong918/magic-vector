package com.data.domain.vo.message

import java.io.Serializable

class MessageContactItemVo : Serializable {
    // 头像
    var avatarUrl: String? = ""
    // 名称
    var name: String = ""
    // 消息概览
    private var messagePreview: String? = ""
    // 时间 yyyy-MM-dd HH:mm:ss
    var time: String? = ""
    // 未读消息条数 （注意0条的时候隐藏view）
    var unreadCount: Int = 0

    fun setByThat(that : MessageContactItemVo){
        this.avatarUrl = that.avatarUrl
        this.name = that.name
        this.messagePreview = that.messagePreview
        this.time = that.time
        this.unreadCount = that.unreadCount
    }

    fun setMessagePreview(content: String) {
        // 长度小于20就全部，大于20就裁剪20 + ...
        messagePreview =
            if (content.length <= 20) {
                content
            } else {
                content.substring(0, 20) + "..."
            }
    }

    fun getMessagePreview(): String {
        return messagePreview?:""
    }
}