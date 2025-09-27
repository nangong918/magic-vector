package com.view.appview.chat

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.core.baseutil.image.ImageLoadUtil
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.chat.MessageTypeEnum
import com.view.appview.databinding.ViewSendMessageItemBinding

class SenderViewHolder : RecyclerView.ViewHolder{

    constructor(binding: ViewSendMessageItemBinding) : super(binding.root){
        this.binding = binding

        binding.tvMessage.text = ""
        binding.tvTime.text = ""
        binding.imgvMessage.visibility = View.GONE
    }

    private val binding: ViewSendMessageItemBinding

    fun bindAo(ao : ChatItemAo){
        binding.tvMessage.text = ao.vo.content
        binding.tvTime.text = ao.vo.time?:"-"

        if (ao.vo.messageType == MessageTypeEnum.IMAGE.value){
            binding.imgvMessage.visibility = View.VISIBLE
            ImageLoadUtil.loadImageViewByResource(
                ao.vo.imgUrl,
                binding.imgvMessage
            )
        }
        else {
            binding.imgvMessage.visibility = View.GONE
        }
    }

    fun setChatMessageClick(onChatMessageClick : OnChatMessageClick){
        // todo
    }
}