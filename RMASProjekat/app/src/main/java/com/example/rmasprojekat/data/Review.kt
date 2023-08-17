package com.example.rmasprojekat.data

data class Review (
    val user: String,
    val rating: Int,
    val text: String,
    val likes: Int,
    val dislikes: Int,
)