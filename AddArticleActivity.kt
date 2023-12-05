package com.example.hyeseong.home

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.example.hyeseong.DBKey.Companion.DB_ARTICLES
import com.example.hyeseong.R

class AddArticleActivity : AppCompatActivity() {


    private val auth: FirebaseAuth by lazy {
        Firebase.auth
    }
    private val storage: FirebaseStorage by lazy {
        Firebase.storage
    }

    private val articleDB: DatabaseReference by lazy {
        Firebase.database.reference.child(DB_ARTICLES)
    }
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_home, menu)
        return true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_article)



        initSubmitButton()

    }

    private fun initSubmitButton() {
        findViewById<Button>(R.id.submitButton).setOnClickListener {
            showProgress()

            val title = findViewById<EditText>(R.id.titleEditText).text.toString()
            val price = findViewById<EditText>(R.id.priceEditText).text.toString()
            val content = findViewById<EditText>(R.id.contentEditText).text.toString()
            val sellState = findViewById<EditText>(R.id.sellEditText).text.toString()
            val sellerId = auth.currentUser?.uid.orEmpty()

            uploadArticle(sellerId, title, price,content,sellState,"")
            hideProgress()




        }
    }


    private fun uploadArticle(sellerId: String, title: String, price: String, content: String,sellState : String,imageUrl: String) {
        val model = ArticleModel(sellerId, title, System.currentTimeMillis(), "${price}원",content,sellState, imageUrl)


        articleDB.push().setValue(model)

        hideProgress()
        finish()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            1010 -> {

                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    Toast.makeText(this, "권한을 거부하셨습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }


    private fun showProgress() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = true

    }

    private fun hideProgress() {
        findViewById<ProgressBar>(R.id.progressBar).isVisible = false
    }



}