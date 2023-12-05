package com.example.hyeseong.chatList

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.hyeseong.DBKey.Companion.CHILD_CHAT
import com.example.hyeseong.DBKey.Companion.DB_USERS
import com.example.hyeseong.R
import com.example.hyeseong.chatdetail.ChatRoomActivity
import com.example.hyeseong.databinding.FragmentChatlistBinding
import com.example.hyeseong.home.ArticleAdapter

class ChatListFragment : Fragment(R.layout.fragment_chatlist) {

    private lateinit var binding: FragmentChatlistBinding
    private lateinit var chatListAdapter: ChatListAdapter
    private val chatRoomList = mutableListOf<ChatListItem>()

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragmentChatlistBinding = FragmentChatlistBinding.bind(view)

        binding = fragmentChatlistBinding

        initchartListAdapter()

        chatRoomList.clear()

        initChartRecyclerView()

        initChatDB()
    }

    private fun initchartListAdapter() {
        chatListAdapter = ChatListAdapter(onItemClicked = { chatRoom ->
            context?.let {
                val intent = Intent(it, ChatRoomActivity::class.java)
                intent.putExtra("chatKey", chatRoom.key)
                startActivity(intent)
            }

        })
    }

    private fun initChartRecyclerView() {
        binding.chatListRecyclerView.adapter = chatListAdapter
        binding.chatListRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun initChatDB() {
        val firebaseUser = auth.currentUser ?: return

        val chatDB =
            Firebase.database.reference.child(DB_USERS).child(firebaseUser.uid).child(CHILD_CHAT)

        chatDB.addListenerForSingleValueEvent(object : ValueEventListener {
            @SuppressLint("NotifyDataSetChanged")
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val model = it.getValue(ChatListItem::class.java)
                    model ?: return
                    chatRoomList.add(model)
                }
                chatListAdapter.submitList(chatRoomList)
                chatListAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}

        })
    }


    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        chatListAdapter.notifyDataSetChanged()
    }

}