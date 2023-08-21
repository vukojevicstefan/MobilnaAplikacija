package com.example.rmasprojekat.data

import com.google.firebase.Timestamp
import java.io.Serializable

data class LocationData(
    var id: String,
    val name: String,
    val type: String,
    val address: String,
    val longitude: Double,
    val latitude: Double,
    var reviews: MutableList<Review>,
    var avgRating: Double,
    var reviewCount: Int,
    val photos: MutableList<String>,
    val timeCreated: Timestamp,
    val author: String
) : Serializable {

    // Custom property to store timeCreated as Long
    val timeCreatedMillis: Long
        get() = timeCreated.seconds * 1000 + timeCreated.nanoseconds / 1000000

    constructor() : this("", "", "", "", 0.0, 0.0, mutableListOf(), 0.0, 0, mutableListOf(), Timestamp.now(), "")
    companion object {
        // Custom factory method to create LocationData from Long and Timestamp
        fun createFromMillis(id: String, name: String, type: String, address: String, longitude: Double, latitude: Double, reviews: MutableList<Review>, avgRating: Double, reviewCount: Int, photos: MutableList<String>, timeCreatedMillis: Long, author: String
        ): LocationData {
            val seconds = (timeCreatedMillis / 1000)
            val nanoseconds = ((timeCreatedMillis % 1000) * 1000000).toInt()
            val timestamp = Timestamp(seconds, nanoseconds)
            return LocationData(id, name, type, address, longitude, latitude, reviews, avgRating, reviewCount, photos, timestamp, author
            )
        }
    }
}

