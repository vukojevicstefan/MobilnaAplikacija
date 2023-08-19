package com.example.rmasprojekat.data

import java.io.Serializable

data class LocationData(
    val id: String,
    val name: String,
    val type: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    var reviews: MutableList<Review>,
    var avgRating: Double,
    var reviewCount: Int,
    val photos: MutableList<String>
) : Serializable {
    constructor() : this("", "", "", "", 0.0, 0.0, mutableListOf(), 0.0, 0, mutableListOf())
}
