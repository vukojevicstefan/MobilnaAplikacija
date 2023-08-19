package com.example.rmasprojekat.data

import java.io.Serializable

data class Image(
    val id: String,
    val path: String,
    var likes: Int = 0,
    var dislikes: Int = 0,
    val user: String
) : Serializable{
    constructor():this("","",0,0,"")
}
