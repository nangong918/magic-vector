package com.view.appview.chat

import android.text.TextUtils
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.core.baseutil.image.ImageLoadUtil
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.chat.MessageTypeEnum
import com.view.appview.databinding.ViewReceivedMessageItemBinding

class ReceiverViewHolder : RecyclerView.ViewHolder{

    constructor(binding: ViewReceivedMessageItemBinding) : super(binding.root){
        this.binding = binding

        binding.tvMessage.text = ""
        binding.tvTime.text = ""
        binding.imgvAvatar.setImageResource(com.view.appview.R.mipmap.logo)
        binding.imgvMessage.visibility = View.GONE
    }

    private val binding: ViewReceivedMessageItemBinding

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

    fun bindAvatar(avatarUrl: String?){
        if (TextUtils.isEmpty(avatarUrl)){
            return
        }
        ImageLoadUtil.loadImageViewByResource(
            avatarUrl,
            binding.imgvAvatar
        )
    }

    fun setChatMessageClick(onChatMessageClick : OnChatMessageClick){
        // todo
    }
}