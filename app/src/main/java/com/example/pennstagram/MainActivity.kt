package com.example.pennstagram

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.*
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private val POSTS = "posts"
    private var feedId: MutableList<String> = ArrayList()
    private var feed: MutableList<Post> = ArrayList()
    lateinit var database : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        database = FirebaseDatabase.getInstance().reference
        posts.layoutManager = LinearLayoutManager(this)
        posts.adapter = PostAdapter(this, feed)
        loadPosts()
        new_post.setOnClickListener {
            val intent = Intent(this, NewPostActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loadPosts() {
        database.child(POSTS).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot : DataSnapshot) {
                dataSnapshot.children.forEach{
                    val post = it.getValue<Post>(Post::class.java)
                    if (!feedId.contains(post!!.uuid)) {
                        feedId.add(post.uuid)
                        feed.add(post)
                    }
                }
                posts.adapter?.notifyDataSetChanged()
            }

            override fun onCancelled(error : DatabaseError) {
                Log.e("error", "Error occurred loading posts", error.toException())
            }

        })
    }

}