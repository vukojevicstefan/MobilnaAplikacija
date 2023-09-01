@file:Suppress("DEPRECATION")

package com.example.rmasprojekat.viewmodels

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.util.Log
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.LocationData
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class MapViewModel(private val currentUserViewModel: CurrentUserViewModel, private val markersViewModel: MarkersViewModel, private val filteredMarkersViewModel:FilteredMarkersViewModel) : ViewModel() {
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    fun newWaypoint(context: Context, activity: Activity) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val dialogView = LayoutInflater.from(context)
                        .inflate(R.layout.marker_dialog, null)
                    val nameEditText = dialogView.findViewById<EditText>(R.id.marker_name_edittext)
                    val typeSpinner = dialogView.findViewById<Spinner>(R.id.marker_type_spinner)

                    val markerTypes = arrayOf("Restaurant", "Coffee Shop", "Fast Food", "Hotel", "Park", "Gas Station", "Other")
                    val typeAdapter =
                        ArrayAdapter(context, R.layout.spinner_dropdown_item, markerTypes)
                    typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    typeSpinner.adapter = typeAdapter

                    AlertDialog.Builder(context)
                        .setTitle("Add Marker")
                        .setView(dialogView)
                        .setPositiveButton("Add") { _, _ ->
                            val name = nameEditText.text.toString()
                            val type = markerTypes[typeSpinner.selectedItemPosition]

                            val address = getAddressFromLocation(location, context).toString()

                            val currentTime = Timestamp.now()
                            val user = currentUserViewModel.currentUser.value?.username
                            if (user != null) {
                                 if (user.isNotEmpty()) {
                                    val markerDetails = LocationData(id = "", name = name, latitude = location.latitude, longitude = location.longitude, address = address, type = type, photos = mutableListOf(), reviews = mutableListOf(), avgRating = 0.0, reviewCount = 0, timeCreated = currentTime, author = user
                                    )
                                    val db = FirebaseFirestore.getInstance()
                                    val collectionRef = db.collection("Markers")
                                    // Add the marker details to Firestore and get the generated document reference
                                    collectionRef.add(markerDetails)
                                        .addOnSuccessListener { documentReference ->
                                            // Handle success
                                            val data = hashMapOf("id" to documentReference.id)
                                            markerDetails.id = documentReference.id
                                            db.collection("Markers").document(documentReference.id)
                                                .set(data, SetOptions.merge())
                                                .addOnSuccessListener {
                                                    // Handle success
                                                    readMarkersList(context)
                                                    Toast.makeText(context, "Marker added.", Toast.LENGTH_SHORT).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    // Handle Firestore write failure
                                                    Toast.makeText(context, "Failed to add marker: ${e.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            // Handle Firestore add failure
                                            Toast.makeText(context, "Failed to add marker: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    // Handle the case where userId is empty
                                    Toast.makeText(context, "User ID is empty.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Handle the case where location is null
                    Toast.makeText(
                        context,
                        "Location not available.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }//End of newWaypoint()
    private fun getAddressFromLocation(location: Location, context: Context): String? {
        val geocoder = Geocoder(context)
        val addresses: List<Address>? =
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return addresses?.get(0)?.getAddressLine(0)
    }
    fun readMarkersList(context:Context) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("Markers")
        collectionRef.get()
            .addOnSuccessListener { documents ->
                val markers: MutableList<LocationData> = mutableListOf()
                for (document in documents) {
                    val location = document.toObject(LocationData::class.java)
                    markers.add(location)
                }
                markersViewModel.setMarkers(markers)
            }
            .addOnFailureListener {
                Toast.makeText(context, "Error getting data.", Toast.LENGTH_SHORT).show()
            }
    }
    fun getMarkers(): List<LocationData> {
        return markersViewModel.getMarkers()
    }
    fun applyFilters(
        filterAuthor: String,
        filterType: String,
        filterDate: String,
        selectedRadius: Int,
        currentLoc: LatLng
    ) {
        val filteredList = getMarkers().filter { location ->
            var authorMatch = location.author.contains(filterAuthor, ignoreCase = true)
            var typeMatch = location.type.contains(filterType, ignoreCase = true)
            val dateMatch =
                filterDate.isEmpty() || isDateInRange(location.timeCreated.toDate(), filterDate)

            if (filterAuthor.isBlank()) {
                authorMatch = true
            }
            if (filterType == "Any Type") {
                typeMatch = true
            }

            authorMatch && typeMatch && dateMatch && calculateDistance(
                currentLoc.latitude,
                currentLoc.longitude,
                location.latitude,
                location.longitude
            ) < selectedRadius
        }
        filteredMarkersViewModel.setMarkers(filteredList)
    }
    private fun isDateInRange(locationDate: Date, filterDate: String): Boolean {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val filterDateParts = filterDate.split(" - ") // Split the date range string

        try {
            if (filterDateParts.size == 2) {
                val startDate = dateFormat.parse(filterDateParts[0])
                val endDate = dateFormat.parse(filterDateParts[1])

                if (startDate != null && endDate != null) {
                    return locationDate>=startDate && locationDate<=endDate
                }
            }
        } catch (e: ParseException) {
            Log.d("ParseException",e.message.toString())
        }
        return false
    }
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radiusOfEarth = 6371 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad
        val a = Math.sin(dLat / 2)
            .pow(2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2).pow(2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return radiusOfEarth * c
    }
    fun getFilteredMarkers(): List<LocationData> {
        return filteredMarkersViewModel.getMarkers()
    }
}