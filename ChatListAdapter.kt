package com.example.hyeseong.chatList

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.hyeseong.databinding.ItemArticleBinding
import com.example.hyeseong.databinding.ItemChatlistBinding
import com.example.hyeseong.home.ArticleModel
import java.text.SimpleDateFormat
import java.util.*

class ChatListAdapter(val onItemClicked: (ChatListItem) -> Unit) :
    ListAdapter<ChatListItem, ChatListAdapter.ViewHolder>(diffUtil) {

    inner class ViewHolder(private val binding: ItemChatlistBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SimpleDateFormat")
        fun bind(chatListItem: ChatListItem) {

            binding.root.setOnClickListener {
                onItemClicked(chatListItem)
            }

            binding.chatRoomTitleTextView.text = chatListItem.itemTitle

        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemChatlistBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    companion object {
        val diffUtil = object : DiffUtil.ItemCallback<ChatListItem>() {
            override fun areItemsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
                return oldItem.key == newItem.key
            }

            override fun areContentsTheSame(oldItem: ChatListItem, newItem: ChatListItem): Boolean {
                // equals 비교;
                return oldItem == newItem

            }

        }
    }
}