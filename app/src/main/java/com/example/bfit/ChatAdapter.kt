package com.example.bfit

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val chatMessages: List<ChatMessage>) :
    RecyclerView.Adapter<ChatAdapter.ChatMessageViewHolder>() {

    companion object {
        private const val VIEW_TYPE_USER = 1
        private const val VIEW_TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (chatMessages[position].isUser) VIEW_TYPE_USER else VIEW_TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatMessageViewHolder {
        val layoutId = if (viewType == VIEW_TYPE_USER) R.layout.list_item_chat_user else R.layout.list_item_chat_bot
        val view = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
        return ChatMessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatMessageViewHolder, position: Int) {
        holder.messageText.text = chatMessages[position].text
    }

    override fun getItemCount(): Int {
        return chatMessages.size
    }

    class ChatMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.messageText)
    }
}
