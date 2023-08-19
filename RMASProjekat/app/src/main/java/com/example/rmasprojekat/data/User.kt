package com.example.rmasprojekat.data

import java.io.Serializable

data class User(
    var id:String,
    var email: String,
    var username: String,
    var firstName: String,
    var lastName: String,
    var phoneNumber: String,
    var score: Int,
    var likedReviews: MutableList<String> = mutableListOf()
) : Serializable {

    constructor() : this("","", "", "", "", "",0, mutableListOf()) {

    }
}
