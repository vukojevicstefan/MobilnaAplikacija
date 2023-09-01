package com.example.rmasprojekat.viewmodels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.Review
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class LocationViewModel(private val userViewModel: CurrentUserViewModel): ViewModel()  {

    private var _currentLocation = LocationData()
    val currentLocation: LocationData = _currentLocation
    fun setCurrentLocation(location:LocationData) {
        _currentLocation = location
    }
    fun getReviews(locationId: String, callback: (List<Review>?) -> Unit) {
        val db = Firebase.firestore
        val reviewsCollection =
            db.collection("Markers").document(locationId).collection("reviews")
        reviewsCollection.get()
            .addOnSuccessListener { documents ->
                val reviewsList = mutableListOf<Review>()
                for (document in documents) {
                    val review = document.toObject(Review::class.java)
                    reviewsList.add(review)
                }
                callback(reviewsList) // Success, pass the list of reviews
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error fetching reviews: ${e.message}")
                callback(null)
            }
    }
    fun addReview(rating:Int, comment:String,locationId: String, context: Context){
    val currentUser=userViewModel.currentUser.value
    var newReview = currentUser?.let {
        Review(
        id = "",
        user = it.username,
        rating = rating,
        text = comment,
        likes = 0,
        markerId = locationId
    )
    }

    val db = Firebase.firestore
    val locationRef = db.collection("Markers").document(locationId)
    val reviewsCollection = locationRef.collection("reviews")


        if (newReview != null) {
            reviewsCollection.add(newReview)
                .addOnSuccessListener { documentReference ->
                    val revC = hashMapOf(
                        "reviewCount" to currentLocation.reviewCount
                    )
                    db.collection("Markers").document(locationId)
                        .set(revC, SetOptions.merge())

                    //Giving documentId to id property of reviews subcollection
                    val data = hashMapOf("id" to documentReference.id)

                    db.collection("Markers").document(locationId)
                        .collection("reviews").document(documentReference.id)
                        .set(data, SetOptions.merge())

                    newReview.id = documentReference.id
                    currentLocation.reviews.add(newReview)
                    addToAuthorScore(currentLocation.author)
                    // Close the dialog
                }
                .addOnFailureListener {
                    Toast.makeText(
                        context,
                        "Error occurred saving your review, please try again!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
    fun getLocationFromId(locationId:String, callback: (LocationData?) -> Unit){
        val db = Firebase.firestore
        val locationRef = db.collection("Markers").document(locationId)
        locationRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val locationData = documentSnapshot.toObject(LocationData::class.java)
                    if (locationData != null) {
                        setCurrentLocation(locationData)
                        callback(locationData)
                    }else{
                        Log.e("MyApp", "Error getting location data: Location is null")
                        callback(null)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error getting location data: $e")
                callback(null)
            }
    }
    fun checkIfUserAlreadyHasReview(reviews: List<Review>):Boolean{
        val user=userViewModel.currentUser.value
        val username= user?.username
        for (review in reviews) {
            if (review.user == username) {
                return true
            }
        }
        return false
    }
    fun addToAuthorScore(authorUsername: String) {
        val db = Firebase.firestore
        val usersCollectionRef = db.collection("Users")

        // Query the Users collection to find the user with the matching username
        usersCollectionRef.whereEqualTo("username", authorUsername)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (documentSnapshot in querySnapshot) {
                    val userRef = usersCollectionRef.document(documentSnapshot.id)
                    userRef.get().addOnSuccessListener { innerDocumentSnapshot ->
                        if (innerDocumentSnapshot.exists()) {
                            var score = innerDocumentSnapshot.getLong("score") ?: 0
                            userRef.update("score", ++score)
                        }
                    }
                }
            }
    }
}