package com.view.appview.chat

import androidx.recyclerview.widget.RecyclerView
import com.data.domain.ao.chat.ChatItemAo
import com.view.appview.databinding.ViewSendMessageItemBinding

class SenderViewHolder : RecyclerView.ViewHolder{

    constructor(binding: ViewSendMessageItemBinding) : super(binding.root){
        this.binding = binding
    }

    private val binding: ViewSendMessageItemBinding

    fun bindAo(ao : ChatItemAo){

    }

    fun setChatMessageClick(onChatMessageClick : OnChatMessageClick){

    }
}