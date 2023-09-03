package com.example.rmasprojekat.viewmodels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.Review
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import java.text.DecimalFormat

class LocationViewModel(private val userViewModel: CurrentUserViewModel) : ViewModel() {

    // Firebase Firestore instance
    private val db = Firebase.firestore

    // MutableLiveData to hold the current location data
    private val _currentLocation = MutableLiveData<LocationData>()
    val currentLocation: LiveData<LocationData> = _currentLocation

    // Firestore document reference for the current location
    private var locationDocRef: DocumentReference? = null

    // Firestore listener registration to track changes in the current location data
    private var reviewsListenerRegistration: ListenerRegistration? = null

    // Function to set the current location data and set up Firestore listeners
    private fun setCurrentLocation(location: LocationData, callback: (Boolean?) -> Unit) {
        _currentLocation.value = location
        locationDocRef = db.collection("Markers").document(location.id)

        // Listen for changes in the Firestore document
        reviewsListenerRegistration = locationDocRef?.addSnapshotListener { documentSnapshot, _ ->
            if (documentSnapshot != null && documentSnapshot.exists()) {
                // Update the LiveData with the latest data
                val updatedLocation = documentSnapshot.toObject(LocationData::class.java)
                _currentLocation.postValue(updatedLocation!!)
            }
        }
        callback(true)
    }

    // Function to fetch reviews for the current location
    private fun getReviews() {
        val locationId = currentLocation.value?.id
        if (locationId != null) {
            if (locationId.isNotEmpty()) {
                val db = Firebase.firestore
                val reviewsCollection = db.collection("Markers").document(locationId)
                    .collection("reviews")

                // Fetch reviews from Firestore
                reviewsCollection.get()
                    .addOnSuccessListener { documents ->
                        val currentLocationData = currentLocation.value!!
                        for (document in documents) {
                            val review = document.toObject(Review::class.java)
                            currentLocationData.reviews.add(review)
                            currentLocationData.reviewCount++
                        }
                        _currentLocation.value = currentLocationData
                    }
                    .addOnFailureListener { e ->
                        Log.e("MyApp", "Error fetching reviews: ${e.message}")
                        // Handle the failure case
                    }
            } else {
                Log.e("MyApp", "Invalid locationId")
                // Handle the case where locationId is invalid
            }
        }
    }

    // Function to add a new review for a location
    fun addReview(rating: Int, comment: String, locationId: String, context: Context) {
        val currentUser = userViewModel.currentUser.value
        val newReview = currentUser?.let {
            Review(
                id = "",
                user = it.username,
                rating = rating,
                text = comment,
                likes = 0,
                markerId = locationId
            )
        }

        // Firestore references
        val db = Firebase.firestore
        val locationRef = db.collection("Markers").document(locationId)
        val reviewsCollection = locationRef.collection("reviews")

        // Update review count and add the new review to the current location
        currentLocation.value!!.reviewCount++
        if (newReview != null) {
            currentLocation.value!!.reviews.add(newReview)
        }

        // Add the new review to Firestore
        if (newReview != null) {
            reviewsCollection.add(newReview)
                .addOnSuccessListener { documentReference ->
                    // Update review count in the parent document
                    val revC = hashMapOf(
                        "reviewCount" to (currentLocation.value?.reviewCount ?: 0)
                    )
                    db.collection("Markers").document(locationId)
                        .set(revC, SetOptions.merge())

                    // Set the document ID in the sub-collection
                    val data = hashMapOf("id" to documentReference.id)
                    db.collection("Markers").document(locationId)
                        .collection("reviews").document(documentReference.id)
                        .set(data, SetOptions.merge())

                    // Update ratings and author's score
                    newReview.id = documentReference.id
                    getReviews()
                    updateMarkerRating(newReview.rating)
                    currentLocation.value?.author?.let { addToAuthorScore() }

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

    // Function to fetch location data from Firestore based on its ID
    fun getLocationFromId(locationId: String, callback: (Boolean?) -> Unit) {
        val db = Firebase.firestore
        val locationRef = db.collection("Markers").document(locationId)
        locationRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val locationData = documentSnapshot.toObject(LocationData::class.java)
                    if (locationData != null) {
                        setCurrentLocation(locationData) {
                            if (it == true) {
                                getReviews()
                                callback(true)
                            }
                        }
                    } else {
                        Log.e("MyApp", "Error getting location data: Location is null")
                        callback(false)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error getting location data: $e")
                callback(false)
            }
    }

    // Function to check if the user has already reviewed the current location
    fun checkIfUserAlreadyHasReview(reviews: List<Review>): Boolean {
        val user = userViewModel.currentUser.value
        val username = user?.username
        for (review in reviews) {
            if (review.user == username) {
                return true
            }
        }
        return false
    }

    // Function to increment the author's score
    fun addToAuthorScore() {
        val db = Firebase.firestore
        val usersCollectionRef = db.collection("Users")

        // Query the Users collection to find the user with the matching username
        usersCollectionRef.whereEqualTo("username", currentLocation.value!!.author)
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

    // Function to update the location's average rating
    private fun updateMarkerRating(newRating: Int) {
        var sumOfRatings: Double = newRating.toDouble()
        for (review in currentLocation.value!!.reviews) {
            sumOfRatings += review.rating
        }
        val decimalFormat = DecimalFormat("#.00")
        val formattedAvgRating = decimalFormat.format(calculateAvgRating()).toDouble()
        currentLocation.value!!.avgRating = formattedAvgRating
        updateAvgRatingInFirestore()
    }

    // Function to calculate the average rating of the location
    private fun calculateAvgRating(): Double {
        val reviews = currentLocation.value?.reviews
        if (reviews != null) {
            if (reviews.isEmpty() || currentLocation.value?.reviewCount == 0) {
                return 0.0
            }
        }
        var totalRating = 0.0
        if (reviews != null) {
            for (review in reviews) {
                totalRating += review.rating
            }
        }
        return totalRating / reviews!!.size
    }

    // Function to update the average rating in Firestore
    private fun updateAvgRatingInFirestore() {
        if (currentLocation.value?.id?.isNotEmpty() == true) {
            val db = FirebaseFirestore.getInstance()
            val collectionRef = db.collection("Markers")
            val documentRef = collectionRef.document(currentLocation.value!!.id)

            val data = hashMapOf("avgRating" to currentLocation.value!!.avgRating)

            documentRef
                .set(data, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("Update Avg Rating", "AvgRating updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("Update Avg Rating", "Error updating avgRating: ${e.message}")
                }
        } else {
            Log.e("Update Avg Rating", "Invalid currentLocation.id")
        }
    }

    // Function called when the ViewModel is cleared (e.g., when the associated UI component is destroyed)
    override fun onCleared() {
        super.onCleared()
        reviewsListenerRegistration?.remove()
    }
}
