package com.example.hyeseong.home

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.example.hyeseong.DBKey.Companion.CHILD_CHAT
import com.example.hyeseong.DBKey.Companion.DB_ARTICLES
import com.example.hyeseong.DBKey.Companion.DB_USERS
import com.example.hyeseong.R
import com.example.hyeseong.chatList.ChatListItem
import com.example.hyeseong.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {

    private lateinit var articleDB: DatabaseReference
    private lateinit var userDB: DatabaseReference
    private lateinit var articleAdapter: ArticleAdapter

    private val articleList = mutableListOf<ArticleModel>()

    private val listener = object : ChildEventListener {
        override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
            val articleModel = snapshot.getValue(ArticleModel::class.java)
            articleModel ?: return

            articleList.add(articleModel)
            articleAdapter.submitList(articleList)
        }

        override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onChildRemoved(snapshot: DataSnapshot) {}

        override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}

        override fun onCancelled(error: DatabaseError) {}

    }

    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }

    private var binding: FragmentHomeBinding? = null


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("sslee", "onViewCreated")

        val fragmentHomeBinding = FragmentHomeBinding.bind(view)
        binding = fragmentHomeBinding

        articleList.clear()

        initDB()

        initArticleAdapter(view)

        initArticleRecyclerView()

        initFloatingButton(view)

        initListener()
    }

    private fun initListener() {
        articleDB.addChildEventListener(listener)

        val toggleButton = binding?.toggleMyItemsButton

        toggleButton?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                val filteredList = articleList.filter { it.sellerId != auth.currentUser?.uid }
                articleAdapter.submitList(filteredList)
            } else {
                articleAdapter.submitList(articleList)
            }
        }
    }

    private fun initFloatingButton(view: View) {
        // 플로팅 버튼;
        binding!!.addFloatingButton.setOnClickListener {
            context?.let {
                if (auth.currentUser != null) {
                    val intent = Intent(it, AddArticleActivity::class.java)
                    startActivity(intent)
                } else {
                    Snackbar.make(view, "로그인 후 사용가능", Snackbar.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun initArticleRecyclerView() {
        binding?:return

        binding!!.articleRecyclerView.layoutManager = LinearLayoutManager(context)
        binding!!.articleRecyclerView.adapter = articleAdapter
    }

    private fun initArticleAdapter(view: View) {
        articleAdapter = ArticleAdapter(onItemClicked = { articleModel ->
            if(auth.currentUser != null){
                if(auth.currentUser?.uid != articleModel.sellerId){
                    val chatRoom = ChatListItem(
                        buyerId = auth.currentUser?.uid.toString(),
                        sellerId = articleModel.sellerId,
                        itemTitle = articleModel.title,
                        key = System.currentTimeMillis()
                    )

                    userDB.child(auth.currentUser!!.uid)
                        .child(CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    userDB.child(articleModel.sellerId)
                        .child(CHILD_CHAT)
                        .push()
                        .setValue(chatRoom)

                    Snackbar.make(view, "채팅방 생성완료. 채팅탭에서 보기.", Snackbar.LENGTH_LONG).show()

                }else{
                    Snackbar.make(view, "본인이 올린 아이템입니다.", Snackbar.LENGTH_LONG).show()
                }
            }else{
                Snackbar.make(view, "로그인 후 이용가능", Snackbar.LENGTH_LONG).show()
            }


        })
    }

    private fun initDB() {
        articleDB = Firebase.database.reference.child(DB_ARTICLES)
        userDB = Firebase.database.reference.child(DB_USERS)
    }

    override fun onDestroy() {
        super.onDestroy()

        articleDB.removeEventListener(listener)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onResume() {
        super.onResume()

        articleAdapter.notifyDataSetChanged()
    }

    private fun setArticleSample() {
        articleAdapter.submitList(mutableListOf<ArticleModel>().apply {
            add(ArticleModel("0", "AA", 200000, "3000원","좋아요","selling", ""))
            add(ArticleModel("0", "B", 30000, "6600원","싸요", "sell off",""))
        })
    }

}