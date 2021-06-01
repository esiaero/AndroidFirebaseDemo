package com.example.pennstagram

data class Post (
    val imageUrl : String = "",
    val description : String = "",
    val date : String = "",
    var uuid : String = ""
)