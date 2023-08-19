package com.example.rmasprojekat.data

import java.io.Serializable

data class Review (
    var id: String,
    val user: String,
    val rating: Int,
    val text: String,
    var likes: Int,
    val markerId:String
):Serializable{
    constructor() : this("","",0,"",0,""  )
}
