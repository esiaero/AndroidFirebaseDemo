package com.example.pennstagram

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.tasks.Continuation
import com.google.android.gms.tasks.Task
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.new_post.*
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

class NewPostActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 1
    private val POSTS = "posts"
    private var picture = false

    lateinit var storage : StorageReference
    lateinit var database : DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.new_post)
        storage = FirebaseStorage.getInstance().reference
        database = FirebaseDatabase.getInstance().reference
        cancel_button.setOnClickListener {
            val returnToMain = Intent(this, MainActivity::class.java)
            startActivity(returnToMain)
        }
        take_picture.setOnClickListener{
            dispatchTakePictureIntent()
        }

        publish_post.setOnClickListener {
            val description = input_description.text.toString()
            if (description.isEmpty()) {
                Toast.makeText(this, "Need description", Toast.LENGTH_SHORT).show()
            } else {
                val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.LONG).withZone(ZoneId.systemDefault())
                val formattedDate = LocalDateTime.now().format(formatter)
                savePost(description, formattedDate)
                val returnToMain = Intent(this, MainActivity::class.java)
                startActivity(returnToMain)
            }
        }
    }

    private fun havePermissions() : Boolean {
        return !((ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
                    PackageManager.PERMISSION_GRANTED) ||
                 (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED))
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSIONS_REQUEST)
    }

    private fun dispatchTakePictureIntent() {
        if (havePermissions()) {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
                takePictureIntent.resolveActivity(packageManager)?.also {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        } else {
            requestPermissions()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            picture_preview.tag = saveImage(imageBitmap)
            picture_preview.setImageBitmap(imageBitmap)
            picture = true
        }
    }

    private fun savePost(description : String, date : String) {
        if (picture) {
            val file = Uri.fromFile(File(picture_preview.tag.toString()))
            val imageRef = storage.child("images/${file.lastPathSegment}") // for id purpose
            val uploadTask = imageRef.putFile(file)
            uploadTask.continueWithTask(Continuation<UploadTask.TaskSnapshot, Task<Uri>> { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                return@Continuation imageRef.downloadUrl
            }).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val downloadUri = task.result
                    val post = Post(downloadUri.toString(), description, date)
                    val key = database.child(POSTS).push().key!!
                    post.uuid = key
                    database.child(POSTS).child(key).setValue(post)
                }
            }
        } else {
            val post = Post("", description, date)
            val key = database.child(POSTS).push().key!!
            post.uuid = key
            database.child(POSTS).child(key).setValue(post)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            PERMISSIONS_REQUEST-> {
                // If request is cancelled, the result arrays are empty.
                if (!(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    Toast.makeText(this, "Enable camera in settings to use camera", Toast.LENGTH_LONG).show()
                }
                return
            }
            else -> {
                // Ignore all other requests.
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val back = Intent(this, MainActivity::class.java)
        startActivity(back)
    }

    private fun saveImage(myBitmap: Bitmap):String {
        val bytes = ByteArrayOutputStream()
        myBitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes)
        val wallpaperDirectory = File(
            (Environment.getExternalStorageDirectory()).toString())
        // have the object build the directory structure, if needed.
        if (!wallpaperDirectory.exists()) {
            wallpaperDirectory.mkdirs()
        }
        try {
            val f = File(wallpaperDirectory, ((Calendar.getInstance()
                .timeInMillis).toString() + ".jpg"))
            f.createNewFile()
            val fo = FileOutputStream(f)
            fo.write(bytes.toByteArray())
            MediaScannerConnection.scanFile(this, arrayOf(f.path), arrayOf("image/jpeg"),
                null)
            fo.close()

            return f.absolutePath
        } catch (e1: IOException) {
            e1.printStackTrace()
        }
        return ""
    }
}