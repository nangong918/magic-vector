package com.view.appview.message

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.data.domain.OnPositionItemClick
import com.data.domain.ao.message.MessageContactItemAo
import com.view.appview.databinding.ViewMessageCardItemBinding
import java.util.Optional

class MessageContactAdapter(
    private val messageContactItemAosPointer: MutableList<MessageContactItemAo>,
    private val onPositionItemClick : OnPositionItemClick
) : RecyclerView.Adapter<MessageCardItemViewHolder>(){
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MessageCardItemViewHolder {
        val binding = ViewMessageCardItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageCardItemViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: MessageCardItemViewHolder,
        position: Int
    ) {
        Optional.of(messageContactItemAosPointer)
            .filter { it.size > position }
            .ifPresent {
                holder.bindAo(it[position])
            }
        holder.setPositionClick(onPositionItemClick)
    }

    override fun getItemCount(): Int {
        return messageContactItemAosPointer.size
    }

}