package com.example.rmasprojekat

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.text.DecimalFormat

class LocationActivity : AppCompatActivity() {
    lateinit var tvLocationName: TextView
    lateinit var tvLocationType: TextView
    lateinit var tvLocationAddress: TextView
    lateinit var tvLatLng: TextView
    lateinit var tvAvgRating: TextView
    lateinit var btnAddReview: Button
    lateinit var btnAddPhoto: Button
    lateinit var recyclerView: RecyclerView
    lateinit var LOC:LocationData
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var reviewsAdapter: ReviewsAdapter

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // Initialize views
        tvLocationName = findViewById(R.id.tvLocationName)
        tvLocationType = findViewById(R.id.tvLocationType)
        tvLocationAddress = findViewById(R.id.tvLocationAddress)
        tvLatLng = findViewById(R.id.tvLatLng)
        tvAvgRating = findViewById(R.id.tvAvgRating)
        btnAddReview = findViewById(R.id.btnAddReview)
        btnAddPhoto = findViewById(R.id.btnAddPhoto)
        recyclerView = findViewById(R.id.reviewsRecyclerView)

        val clickedLocation = intent.getSerializableExtra("clickedLocation") as? LocationData
        if (clickedLocation != null) {
            tvLocationName.text = clickedLocation.name
            tvLocationType.text = clickedLocation.type
            tvLocationAddress.text = clickedLocation.address
            tvLatLng.text = "${clickedLocation.latitude}, ${clickedLocation.longitude}"
            tvAvgRating.text = "Rating ${clickedLocation.avgRating}"

            // Initialize RecyclerView and its adapter
            recyclerView.layoutManager = LinearLayoutManager(this)
            reviewsAdapter = ReviewsAdapter(clickedLocation.reviews)
            recyclerView.adapter = reviewsAdapter

            // Fetch reviews from Firestore
            val db = Firebase.firestore
            val reviewsCollection = db.collection("Markers").document(clickedLocation.id).collection("reviews")
            reviewsCollection.get()
                .addOnSuccessListener { documents ->
                    val reviewsList = mutableListOf<Review>()
                    for (document in documents) {
                        val review = document.toObject(Review::class.java)
                        reviewsList.add(review)
                    }
                    clickedLocation.reviews = reviewsList // Update clickedLocation's reviews
                    reviewsAdapter.updateReviews(clickedLocation.reviews) // Update adapter
                }
                .addOnFailureListener {
                    Log.e("MyApp", "Error fetching reviews: ${it.message}")
                }

            // Set up click listener for the "Add Review" button
            btnAddReview.setOnClickListener {
                addReview(clickedLocation)
            }
        } else {
            Toast.makeText(this, "Location is null", Toast.LENGTH_SHORT).show()
        }

        if (clickedLocation != null) {
            LOC=clickedLocation
            displayImages(clickedLocation.photos)
        }
        btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }
    }//End of onCreate

    private fun addReview(clickedLocation: LocationData) {
        val db = Firebase.firestore
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.layout_marker_details, null)

        // Initialize views in the dialog view
        val tvMarkerTitle = dialogView.findViewById<TextView>(R.id.tv_marker_title)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
        val etComment = dialogView.findViewById<EditText>(R.id.et_comment)
        val btnSubmit = dialogView.findViewById<Button>(R.id.btn_submit)

        // Set initial values for views based on clickedLocation
        tvMarkerTitle.text = clickedLocation.name
        ratingBar.rating = clickedLocation.avgRating.toFloat()

        // Create a dialog
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setView(dialogView)
        val dialog = dialogBuilder.create()
        dialog.show()


        // Handle the submit button click
        btnSubmit.setOnClickListener {
            // Create a new review using the entered data
            returnUserName { username ->
                var newReview = Review(
                    id = "",
                    user = username,
                    rating = ratingBar.rating.toInt(),
                    text = etComment.text.toString(),
                    likes = 0,
                    markerId = clickedLocation.id
                )

            val locationRef = db.collection("Markers").document(clickedLocation.id)
            val reviewsCollection = locationRef.collection("reviews")

            Log.d("MyApp", "Reached point A, ${clickedLocation.id}")

            reviewsCollection.add(newReview)
                .addOnSuccessListener { documentReference ->

                    //Updating average rating
                    updateMarkerRating(clickedLocation,newReview.rating)
                    val avgRat = hashMapOf("avgRating" to clickedLocation.avgRating,
                        "reviewCount" to clickedLocation.reviewCount)
                    db.collection("Markers").document(clickedLocation.id)
                        .set(avgRat,SetOptions.merge())

                    //Giving documentId to id property of reviews subcollection
                    val data = hashMapOf("id" to documentReference.id)

                    db.collection("Markers").document(clickedLocation.id).collection("reviews").document(documentReference.id)
                        .set(data, SetOptions.merge())

                    newReview.id=documentReference.id
                    clickedLocation.reviews.add(newReview)

                    // Update the RecyclerView's dataset and refresh it
                    reviewsAdapter.updateReviews(clickedLocation.reviews)

                    // Close the dialog
                    dialog.dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error occurred saving your review, please try again!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun updateMarkerRating(location:LocationData,newRating:Int){
        location.reviewCount++
        var sumOfRatings:Double=newRating.toDouble()
        for(review in location.reviews){
            sumOfRatings+=review.rating
        }
        val decimalFormat= DecimalFormat("#.00")
        location.avgRating=decimalFormat.format(sumOfRatings/location.reviewCount.toDouble()).toDouble()
        tvAvgRating.text = "Rating ${location.avgRating}"
    }
    private fun toastUsersWhoCommented(location: LocationData?){
        if (location != null) {
            for(review in location.reviews){
                Toast.makeText(this,"User: ${review.user}",Toast.LENGTH_SHORT).show()
            }
        }
        Toast.makeText(this,"NULL",Toast.LENGTH_SHORT).show()
    }
    private fun returnUserName(callback: (String) -> Unit) {
        val user = FirebaseAuth.getInstance().currentUser
        val db = FirebaseFirestore.getInstance()

        if (user != null) {
            db.collection("Users")
                .whereEqualTo("email", user.email)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val u = document.getString("username")
                        if (u != null) {
                            callback(u) // Pass the retrieved username to the callback
                            return@addOnSuccessListener
                        }
                    }
                    callback("Unknown User") // If no username is found, pass the default value
                }
                .addOnFailureListener {
                    callback("Unknown User") // Handle failure by passing the default value
                }
        } else {
            callback("Unknown User") // If user is null, pass the default value
        }
    }
    private fun displayImages(imageUrls: List<String>) {
        val imagesRecyclerView: RecyclerView = findViewById(R.id.imagesRecyclerView)
        imagesRecyclerView.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        val imagesAdapter = ImagesAdapter(imageUrls)
        imagesRecyclerView.adapter = imagesAdapter
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val storage = FirebaseStorage.getInstance()

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val selectedImageUri: Uri? = data.data
            if (selectedImageUri != null) {
                // Upload the selected image to Firebase Storage
                val storageRef = storage.reference
                val imagesRef = storageRef.child("photos/${selectedImageUri.lastPathSegment}")
                val uploadTask = imagesRef.putFile(selectedImageUri)
                Log.d("MyApp", imagesRef.path)
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    // Get the download URL of the uploaded image
                    imagesRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Update the Firestore document with the image URL
                        val db = FirebaseFirestore.getInstance()
                        val markerRef = db.collection("Markers").document(LOC.id)
                        Log.d("MyApp",markerRef.id)
                        markerRef.get()
                            .addOnSuccessListener { documentSnapshot ->
                                val photos = documentSnapshot.get("photos") as? MutableList<String>
                                if (photos != null) {
                                    photos.add(downloadUri.toString()) // Add the image URL to the list
                                    markerRef.update("photos", photos)
                                        .addOnSuccessListener {
                                            Log.d("MyApp", "Document updated with photo URL.")
                                            // After updating the Firestore document, you can refresh the RecyclerView
                                            displayImages(photos)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("MyApp", "Error updating document with photo URL: $e")
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("MyApp", "Error getting document: $e")
                            }
                    }
                }.addOnFailureListener { exception ->
                    Log.e("MyApp", "Error uploading image: $exception")
                }
            }
        }
    }


}