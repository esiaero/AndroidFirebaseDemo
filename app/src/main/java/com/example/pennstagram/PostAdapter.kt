package com.example.pennstagram

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.post.view.*

class PostViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    var image: ImageView = view.image
    var description: TextView = view.description
    var date: TextView = view.date
    var delete: ImageButton = view.delete
    var id : String = ""

    fun bind(item: Post) {
        image.tag = item.imageUrl
        description.text = item.description
        date.text = item.date
        id = item.uuid
    }
}

class PostAdapter(private var activity: Activity, private var feed: MutableList<Post>)
    : RecyclerView.Adapter<PostViewHolder>() {

    private lateinit var storage : FirebaseStorage
    private lateinit var database : DatabaseReference
    private val POSTS = "posts"

    private fun removeAt(holder: PostViewHolder) {
        val position = holder.adapterPosition

        feed.removeAt(position)
        val url : String = holder.image.tag as String
        if (url.isNotEmpty()) {
            val imageStorageRef = storage.getReferenceFromUrl(url)
            imageStorageRef.delete().addOnFailureListener {
                Log.e("error", "bad things happened in storage :(")
            }
        }

        val pic = database.child(POSTS).child(holder.id)
        pic.removeValue().addOnFailureListener {
            Log.e("error", "bad things happened in realtime database :(")
        }
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, feed.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val holder = PostViewHolder(LayoutInflater.from(activity).inflate(R.layout.post, parent, false))
        storage = FirebaseStorage.getInstance()
        database = FirebaseDatabase.getInstance().reference

        holder.delete.setOnClickListener {
            removeAt(holder)
        }
        return holder
    }
    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(feed[position])
        val url = feed[position].imageUrl
        if (url.isNotEmpty()) {
            Picasso.get().load(url).into(holder.image)
        }
    }

    override fun getItemCount(): Int = feed.size
}