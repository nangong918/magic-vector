package com.view.appview.message

import androidx.recyclerview.widget.RecyclerView
import com.core.baseutil.image.ImageLoadUtil
import com.data.domain.constant.OnPositionItemClick
import com.data.domain.constant.ao.MessageContactItemAo
import com.view.appview.databinding.ViewMessageCardItemBinding

class MessageCardItemViewHolder : RecyclerView.ViewHolder {

    constructor(binding: ViewMessageCardItemBinding) : super(binding.root){
        this.binding = binding
    }

    private val binding: ViewMessageCardItemBinding

    private var messageContactItemAo : MessageContactItemAo? = null

    fun bindAo(ao: MessageContactItemAo){
        // 存储data
        messageContactItemAo = ao

        // bind vo
        ImageLoadUtil.loadImageViewByResource(
            ao.vo.avatarUrl,
            binding.imvgAvatar
        )
        binding.tvName.text = ao.vo.name
        binding.tvMessagePreview.text = ao.vo.getMessagePreview()
        binding.tvTime.text = ao.vo.time

        // 未读消息数量
        binding.vMessagePrompt.setMessageNum(ao.vo.unreadCount)
    }

    fun setPositionClick(onPositionItemClick: OnPositionItemClick?) {
        binding.lyMain.setOnClickListener { v ->
            onPositionItemClick?.onPositionItemClick(bindingAdapterPosition)
        }
    }
}