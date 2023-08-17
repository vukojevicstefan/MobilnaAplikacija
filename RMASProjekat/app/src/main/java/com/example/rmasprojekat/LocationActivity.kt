@file:Suppress("DEPRECATION")
package com.example.rmasprojekat

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.RatingBar
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.Review
import com.google.firebase.auth.FirebaseAuth

class LocationActivity : AppCompatActivity() {
    lateinit var tvLocationName: TextView
    lateinit var tvLocationType: TextView
    lateinit var tvLocationAddress: TextView
    lateinit var tvLatitude: TextView
    lateinit var tvLongitude: TextView
    lateinit var tvAvgRating: TextView
    lateinit var btnAddReview: Button

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location)

        // Initialize views
        tvLocationName = findViewById(R.id.tvLocationName)
        tvLocationType = findViewById(R.id.tvLocationType)
        tvLocationAddress = findViewById(R.id.tvLocationAddress)
        tvLatitude = findViewById(R.id.tvLatitude)
        tvLongitude = findViewById(R.id.tvLongitude)
        tvAvgRating = findViewById(R.id.tvAvgRating)
        btnAddReview = findViewById(R.id.btnAddReview)

        val clickedLocation = intent.getSerializableExtra("clickedLocation") as? LocationData
        if (clickedLocation != null) {
            tvLocationName.text = "${clickedLocation.name}"
            tvLocationType.text = "${clickedLocation.type}"
            tvLocationAddress.text = "${clickedLocation.address}"
            tvLatitude.text = "${clickedLocation.latitude}"
            tvLongitude.text = "${clickedLocation.longitude}"
            tvAvgRating.text = "${clickedLocation.avgRating}"

            // Set up click listener for the "Add Review" button
            btnAddReview.setOnClickListener {
                addReview(clickedLocation)
            }
        }
        else{
            Toast.makeText(this,"Location is null", Toast.LENGTH_SHORT).show()
        }
    }
    private fun addReview(clickedLocation: LocationData) {
        if (clickedLocation == null) {
            Toast.makeText(this, "Error occurred.", Toast.LENGTH_SHORT).show()
            return
        }

        // Inflate the layout for the marker details dialog
        val dialogView = layoutInflater.inflate(R.layout.layout_marker_details, null)

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

        val user = FirebaseAuth.getInstance().currentUser
        val username = user?.displayName ?: "Unknown User" // Use the user's display name, fallback to "Unknown User"

        // Handle the submit button click
        btnSubmit.setOnClickListener {
            // Create a new review using the entered data
            val newReview = Review(
                user = username,
                rating = ratingBar.rating.toInt(),
                text = etComment.text.toString(),
                likes = 0,
                dislikes = 0
            )

            // Update the clickedLocation's reviews list with the new review
            clickedLocation.reviews.add(newReview)


            // Close the dialog
            dialog.dismiss()
        }
    }
}
