package com.example.rmasprojekat.fragments

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.rmasprojekat.R
import com.example.rmasprojekat.adapters.ImagesAdapter
import com.example.rmasprojekat.adapters.ReviewsAdapter
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.Review
import com.example.rmasprojekat.databinding.FragmentLocationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.text.DecimalFormat

class LocationFragment : Fragment() {
    private lateinit var tvLocationName: TextView
    private lateinit var tvLocationType: TextView
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvAvgRating: TextView
    private lateinit var btnAddReview: ImageView
    private lateinit var btnAddPhoto: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var LOC: LocationData
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var imagesRecyclerView: RecyclerView

    private lateinit var binding: FragmentLocationBinding

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize views
        tvLocationName = view.findViewById(R.id.tvLocationName)
        tvLocationType = view.findViewById(R.id.tvLocationType)
        tvLocationAddress = view.findViewById(R.id.tvLocationAddress)
        tvAvgRating = view.findViewById(R.id.tvAvgRating)
        btnAddReview = view.findViewById(R.id.btnAddReview)
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        recyclerView = view.findViewById(R.id.reviewsRecyclerView)
        imagesRecyclerView = view.findViewById(R.id.imagesRecyclerView)

        val locationId = arguments?.getString("clickedLocationId")
        if (locationId != null) {

        val db = Firebase.firestore
        val locationRef = db.collection("Markers").document(locationId)
        locationRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    val locationData = documentSnapshot.toObject(LocationData::class.java)
                    if (locationData != null) {
                        updateUI(locationData)
                    }else{
                        Log.e("MyApp", "Error getting location data: Location is null")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error getting location data: $e")
            }
        }

    }
    private fun updateUI(location:LocationData) {
        tvLocationName.text = location.name
        tvLocationType.text = location.type
        tvLocationAddress.text = location.address
        tvAvgRating.text = "Rating ${location.avgRating}"
        (requireActivity() as AppCompatActivity).supportActionBar?.title = location.name

        // Initialize RecyclerView and its adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        reviewsAdapter = ReviewsAdapter(location.reviews)
        recyclerView.adapter = reviewsAdapter

        // Fetch reviews from Firestore
        val db = Firebase.firestore
        val reviewsCollection =
            db.collection("Markers").document(location.id).collection("reviews")
        reviewsCollection.get()
            .addOnSuccessListener { documents ->
                val reviewsList = mutableListOf<Review>()
                for (document in documents) {
                    val review = document.toObject(Review::class.java)
                    reviewsList.add(review)
                }
                location.reviews = reviewsList // Update clickedLocation's reviews
                reviewsAdapter.updateReviews(location.reviews) // Update adapter
            }
            .addOnFailureListener {
                Log.e("MyApp", "Error fetching reviews: ${it.message}")
            }

        calculateAvgRating(location)
        btnAddReview.setOnClickListener {
            addReview(location)
        }

        LOC = location
        displayImages(location.photos)
        btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            displayImages(location.photos)
        }
    }
    private fun addReview(clickedLocation: LocationData) {
        checkIfUserAlreadyHasReview(clickedLocation.reviews.toList()){hasReview->
          if(!hasReview) {
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
          val dialogBuilder = AlertDialog.Builder(requireContext())
          dialogBuilder.setView(dialogView)
          val dialog = dialogBuilder.create()
          dialog.show()


          // Handle the submit button click
          btnSubmit.setOnClickListener {
              btnSubmit.visibility=View.GONE
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


                  reviewsCollection.add(newReview)
                      .addOnSuccessListener { documentReference ->

                          updateMarkerRating(clickedLocation, newReview.rating)
                          val revC = hashMapOf(
                              "reviewCount" to clickedLocation.reviewCount
                          )
                          db.collection("Markers").document(clickedLocation.id)
                              .set(revC, SetOptions.merge())

                          //Giving documentId to id property of reviews subcollection
                          val data = hashMapOf("id" to documentReference.id)

                          db.collection("Markers").document(clickedLocation.id)
                              .collection("reviews").document(documentReference.id)
                              .set(data, SetOptions.merge())

                          newReview.id = documentReference.id
                          clickedLocation.reviews.add(newReview)
                          returnUserName {
                              addToAuthorScore(clickedLocation.author)
                          }
                          // Update the RecyclerView's dataset and refresh it
                          reviewsAdapter.updateReviews(clickedLocation.reviews)
                          updateUI(clickedLocation)
                          // Close the dialog
                          dialog.dismiss()
                      }
                      .addOnFailureListener {
                          Toast.makeText(
                              requireContext(),
                              "Error occurred saving your review, please try again!",
                              Toast.LENGTH_SHORT
                          ).show()
                      }
              }
          }
          }else{
              Toast.makeText(requireContext(),"You already uploaded a review!",Toast.LENGTH_SHORT).show()
          }
        }
    }
    private fun updateMarkerRating(location:LocationData, newRating:Int){
        location.reviewCount++
        var sumOfRatings:Double=newRating.toDouble()
        for(review in location.reviews){
            sumOfRatings+=review.rating
        }
        val decimalFormat= DecimalFormat("#.00")
        location.avgRating=decimalFormat.format(sumOfRatings/location.reviewCount.toDouble()).toDouble()
        tvAvgRating.text = "Rating ${location.avgRating}"
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
                            callback(u)
                            return@addOnSuccessListener
                        }
                    }
                    callback("Unknown User")
                }
                .addOnFailureListener {
                    callback("Unknown User")
                }
        } else {
            callback("Unknown User")
        }
    }
    private fun addToAuthorScore(authorUsername: String) {
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
    private fun checkIfUserAlreadyHasReview(reviews: List<Review>, callback: (Boolean) -> Unit) :Boolean{
        returnUserName { username ->
            for (review in reviews) {
                if (review.user == username) {
                    // User already has a review
                    callback(true)
                    return@returnUserName
                }
            }
            callback(false)
        }
        return false
    }
    private fun displayImages(imageUrls: List<String>) {
        imagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
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
                                            addToAuthorScore(LOC.author)
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
    private fun calculateAvgRating(location: LocationData) {
        val reviews = location.reviews

        if (reviews.isEmpty() || location.reviewCount==0) {
            location.avgRating = 0.0
            return
        }

        var totalRating = 0.0

        for (review in reviews) {
            totalRating += review.rating
        }
        val avgRating = totalRating / reviews.size
        location.avgRating = avgRating
        updateAvgRatingInFirestore(location)
    }
    private fun updateAvgRatingInFirestore(location: LocationData) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("Markers")
        val documentRef = collectionRef.document(location.id)

        val data = hashMapOf("avgRating" to location.avgRating)

        documentRef
            .set(data, SetOptions.merge())
            .addOnFailureListener { e ->
                Log.e("Update Avg Rating", "Error updating avgRating: ${e.message}")
            }
    }
}