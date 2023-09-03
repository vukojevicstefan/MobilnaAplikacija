@file:Suppress("DEPRECATION")

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
    // Declare UI elements
    private lateinit var tvLocationName: TextView
    private lateinit var tvLocationType: TextView
    private lateinit var tvLocationAddress: TextView
    private lateinit var tvAvgRating: TextView
    private lateinit var btnAddReview: ImageView
    private lateinit var btnAddPhoto: ImageView
    private lateinit var recyclerView: RecyclerView
    private val PICK_IMAGE_REQUEST = 1
    private lateinit var reviewsAdapter: ReviewsAdapter
    private lateinit var imagesRecyclerView: RecyclerView

    // Data binding for this fragment
    private lateinit var binding: FragmentLocationBinding

    // ViewModel for handling location-related data and actions
    private lateinit var viewModel: LocationViewModel

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using data binding
        binding = FragmentLocationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize UI elements
        tvLocationName = view.findViewById(R.id.tvLocationName)
        tvLocationType = view.findViewById(R.id.tvLocationType)
        tvLocationAddress = view.findViewById(R.id.tvLocationAddress)
        tvAvgRating = view.findViewById(R.id.tvAvgRating)
        btnAddReview = view.findViewById(R.id.btnAddReview)
        btnAddPhoto = view.findViewById(R.id.btnAddPhoto)
        recyclerView = view.findViewById(R.id.reviewsRecyclerView)
        imagesRecyclerView = view.findViewById(R.id.imagesRecyclerView)

        // Get a reference to the parent activity and set up the ViewModel
        val activity = requireActivity() as MainActivity
        viewModel = LocationViewModel(activity.userViewModel)

        // Retrieve the location ID from the fragment arguments and load location data
        val locationId = arguments?.getString("clickedLocationId")
        if (locationId != null) {
            viewModel.getLocationFromId(locationId) {
                if (it == true)
                    observeLocationData()
                else
                    Toast.makeText(requireContext(), "Error getting data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Update the UI with location data
    private fun updateUI(locationData: LocationData) {
        tvLocationName.text = locationData.name
        tvLocationType.text = locationData.type
        tvLocationAddress.text = locationData.address
        tvAvgRating.text = "Rating ${locationData.avgRating}"
        (requireActivity() as AppCompatActivity).supportActionBar?.title = locationData.name

        // Initialize RecyclerView and its adapter for displaying reviews
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val sortedReviews = locationData.reviews.sortedByDescending { it.rating }
        val activity = requireActivity() as MainActivity
        reviewsAdapter = ReviewsAdapter(sortedReviews.toMutableList(), activity.userViewModel)
        recyclerView.adapter = reviewsAdapter

        // Set up a click listener for adding a review
        btnAddReview.setOnClickListener {
            addReview()
        }

        // Display images related to the location
        displayImages(locationData.photos)

        // Set up a click listener for adding a photo
        btnAddPhoto.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
            // Refresh images after selecting a new one
            displayImages(locationData.photos)
        }
    }

    // Observe changes in location data using ViewModel
    private fun observeLocationData() {
        viewModel.currentLocation.observe(viewLifecycleOwner) { locationData ->
            updateUI(locationData)
        }
    }

    // Allow the user to add a review for the location
    private fun addReview() {
        val currentLocation = viewModel.currentLocation.value
        if (currentLocation != null && !viewModel.checkIfUserAlreadyHasReview(currentLocation.reviews.toList())) {
            val inflater = layoutInflater
            val dialogView = inflater.inflate(R.layout.layout_marker_details, null)

            // Initialize views in the review dialog
            val tvMarkerTitle = dialogView.findViewById<TextView>(R.id.tv_marker_title)
            val ratingBar = dialogView.findViewById<RatingBar>(R.id.rating_bar)
            val etComment = dialogView.findViewById<EditText>(R.id.et_comment)
            val btnSubmit = dialogView.findViewById<Button>(R.id.btn_submit)

            // Set the title of the review dialog based on the current location
            tvMarkerTitle.text = currentLocation.name

            // Create an AlertDialog for adding a review
            val dialogBuilder = AlertDialog.Builder(requireContext())
            dialogBuilder.setView(dialogView)
            val dialog = dialogBuilder.create()
            dialog.show()

            // Set up a click listener for the review submission
            btnSubmit.setOnClickListener {
                btnSubmit.visibility = View.GONE
                viewModel.addReview(
                    ratingBar.rating.toInt(),
                    etComment.text.toString(),
                    currentLocation.id,
                    requireContext()
                )
                // Update the UI with the new review and dismiss the dialog
                observeLocationData()
                dialog.dismiss()
            }
        } else {
            Toast.makeText(requireContext(), "You already uploaded a review!", Toast.LENGTH_SHORT).show()
        }
    }

    // Display images related to the location in a horizontal RecyclerView
    private fun displayImages(imageUrls: List<String>) {
        imagesRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        val imagesAdapter = ImagesAdapter(imageUrls)
        imagesRecyclerView.adapter = imagesAdapter
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val storage = FirebaseStorage.getInstance()
        val currentLocationId = viewModel.currentLocation.value?.id

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
                        val markerRef = db.collection("Markers").document(currentLocationId!!)
                        Log.d("MyApp", markerRef.id)
                        markerRef.get()
                            .addOnSuccessListener { documentSnapshot ->
                                val photos = documentSnapshot.get("photos") as? MutableList<String>
                                if (photos != null) {
                                    photos.add(downloadUri.toString()) // Add the image URL to the list
                                    markerRef.update("photos", photos)
                                        .addOnSuccessListener {
                                            Log.d("MyApp", "Document updated with photo URL.")
                                            viewModel.addToAuthorScore()
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