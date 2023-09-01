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
import com.example.rmasprojekat.MainActivity
import com.example.rmasprojekat.R
import com.example.rmasprojekat.adapters.ImagesAdapter
import com.example.rmasprojekat.adapters.ReviewsAdapter
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.Review
import com.example.rmasprojekat.databinding.FragmentLocationBinding
import com.example.rmasprojekat.viewmodels.LocationViewModel
import com.example.rmasprojekat.viewmodels.MapViewModel
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

    private lateinit var viewModel: LocationViewModel
    private lateinit var currentLocation:LocationData

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

        val activity = requireActivity() as MainActivity
        viewModel = LocationViewModel(activity.userViewModel)
        val locationId = arguments?.getString("clickedLocationId")
        if (locationId != null) {
            viewModel.getLocationFromId(locationId){
                if (it != null) {
                    currentLocation=it
                    updateUI(it)
                }
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

        viewModel.getReviews(location.id){
            if (it != null) {
                location.reviews = it.toMutableList()
                reviewsAdapter.updateReviews(location.reviews) // Update adapter
            } // Update clickedLocation's reviews
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
          if(viewModel.checkIfUserAlreadyHasReview(clickedLocation.reviews.toList())) {
          val inflater = layoutInflater
          val dialogView = inflater.inflate(R.layout.layout_marker_details, null)

          // Initialize views in the dialog view
          val tvMarkerTitle = dialogView.findViewById<TextView>(R.id.tv_marker_title)
          val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
          val etComment = dialogView.findViewById<EditText>(R.id.et_comment)
          val btnSubmit = dialogView.findViewById<Button>(R.id.btn_submit)

          // Set initial values for views based on clickedLocation
          tvMarkerTitle.text = clickedLocation.name

          // Create a dialog
          val dialogBuilder = AlertDialog.Builder(requireContext())
          dialogBuilder.setView(dialogView)
          val dialog = dialogBuilder.create()
          dialog.show()

          btnSubmit.setOnClickListener {
              btnSubmit.visibility=View.GONE
              viewModel.addReview(ratingBar.rating.toInt(),etComment.text.toString(),currentLocation.id, requireContext())
              updateMarkerRating(currentLocation, ratingBar.rating.toInt())
              reviewsAdapter.updateReviews(currentLocation.reviews)
              updateUI(currentLocation)
              dialog.dismiss()
          }
          }else{
              Toast.makeText(requireContext(),"You already uploaded a review!",Toast.LENGTH_SHORT).show()
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
                                            viewModel.addToAuthorScore(LOC.author)
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