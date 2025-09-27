package com.view.appview.chat

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.data.domain.ao.chat.ChatItemAo
import com.data.domain.constant.chat.SendMessageTypeEnum
import com.view.appview.databinding.ViewReceivedMessageItemBinding
import com.view.appview.databinding.ViewSendMessageItemBinding
import java.util.Optional

class ChatMessageAdapter(
    private val chatMessageItemAosPointer: List<ChatItemAo>,
    private val onChatMessageClick : OnChatMessageClick
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var currentAvatarUrl: String? = null

    @SuppressLint("NotifyDataSetChanged")
    fun setCurrentAvatarUrl(avatarUrl: String?) {
        this.currentAvatarUrl = avatarUrl
        notifyDataSetChanged()
    }

    //实现不同的viewType
    override fun getItemViewType(position: Int): Int {
        return Optional.of(chatMessageItemAosPointer)
            .filter { it -> it.size > position }
            .map { it -> it[position].vo.viewType }
            .orElse(SendMessageTypeEnum.VIEW_TYPE_SENDER.value)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)

        when (viewType) {
            SendMessageTypeEnum.VIEW_TYPE_SENDER.value -> {
                val binding: ViewSendMessageItemBinding =
                    ViewSendMessageItemBinding.inflate(inflater, parent, false)
                return SenderViewHolder(binding)
            }
            SendMessageTypeEnum.VIEW_TYPE_RECEIVER.value -> {
                val binding: ViewReceivedMessageItemBinding =
                    ViewReceivedMessageItemBinding.inflate(inflater, parent, false)
                return ReceiverViewHolder(binding)
            }
            else -> {
                throw Exception("ChatMessageAdapter::viewType error")
            }
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        if (position >= chatMessageItemAosPointer.size){
            return
        }
        val ao: ChatItemAo = chatMessageItemAosPointer[position]
        when(ao.vo.viewType){
            SendMessageTypeEnum.VIEW_TYPE_SENDER.value -> {
                (holder as SenderViewHolder).bindAo(ao)
                holder.setChatMessageClick(onChatMessageClick)
            }
            SendMessageTypeEnum.VIEW_TYPE_RECEIVER.value -> {
                (holder as ReceiverViewHolder).bindAo(ao)
                holder.bindAvatar(currentAvatarUrl)
                holder.setChatMessageClick(onChatMessageClick)
            }
            else -> {
                // do nothing
            }
        }
    }

    override fun getItemCount(): Int {
        return chatMessageItemAosPointer.size
    }
}

