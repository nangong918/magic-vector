package com.view.appview.chat

import androidx.recyclerview.widget.RecyclerView
import com.data.domain.ao.chat.ChatItemAo
import com.view.appview.databinding.ViewReceivedMessageItemBinding

class ReceiverViewHolder : RecyclerView.ViewHolder{

    constructor(binding: ViewReceivedMessageItemBinding) : super(binding.root){
        this.binding = binding
    }

    private val binding: ViewReceivedMessageItemBinding

    fun bindAo(ao : ChatItemAo){

    }

    fun setChatMessageClick(onChatMessageClick : OnChatMessageClick){

    }
}