@file:Suppress("DEPRECATION")

package com.example.rmasprojekat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.MarkerListCallback
import com.example.rmasprojekat.data.Review
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, MarkerListCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var gMap: GoogleMap
    private lateinit var map: FrameLayout
    private lateinit var savedMarkers: MutableList<LocationData>
    private lateinit var auth : FirebaseAuth

    private lateinit var addMarker:FloatingActionButton
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        map = findViewById(R.id.map)
        savedMarkers = mutableListOf()
        addMarker=findViewById(R.id.addMarker)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        readMarkersList(this)
        addMarker.setOnClickListener {
            newWaypoint()
        }
        auth=FirebaseAuth.getInstance()
    }
    private fun newWaypoint() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val dialogView = LayoutInflater.from(this@MapsActivity)
                        .inflate(R.layout.marker_dialog, null)
                    val nameEditText = dialogView.findViewById<EditText>(R.id.marker_name_edittext)
                    val typeSpinner = dialogView.findViewById<Spinner>(R.id.marker_type_spinner)

                    val markerTypes = arrayOf("Restaurant", "Coffee Shop", "Fast Food", "Hotel", "Park", "Gas Station", "Other")
                    val typeAdapter = ArrayAdapter(this@MapsActivity, R.layout.spinner_dropdown_item, markerTypes)
                    typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    typeSpinner.adapter = typeAdapter

                    AlertDialog.Builder(this@MapsActivity)
                        .setTitle("Add Marker")
                        .setView(dialogView)
                        .setPositiveButton("Add") { _, _ ->
                            val name = nameEditText.text.toString()
                            val type = markerTypes[typeSpinner.selectedItemPosition]

                            val address = getAddressFromLocation(location).toString()

                            val markerDetails = LocationData(
                                id="",
                                name = name,
                                latitude = location.latitude,
                                longitude = location.longitude,
                                address = address,
                                type = type,
                                photos = mutableListOf(),
                                reviews = mutableListOf(),
                                avgRating = 0.0,
                                reviewCount = 0
                            )

                            val db = FirebaseFirestore.getInstance()
                            val collectionRef = db.collection("Markers")
                            // Add the marker details to Firestore and get the generated document reference
                            collectionRef.add(markerDetails).addOnSuccessListener {
                                val data = hashMapOf("id" to it.id)

                                db.collection("Markers").document(it.id)
                                    .set(data, SetOptions.merge())
                            }


                            readMarkersList(this@MapsActivity)
                            Toast.makeText(
                                this@MapsActivity,
                                "Marker added.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        checkLocationPermission()
        showSavedMarkersOnMap()
    }

    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gMap.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener(this
            ) { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val userLocation = LatLng(latitude, longitude)
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            showUserLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            }
        }
    }

    private fun showSavedMarkersOnMap() {
        if (savedMarkers.isEmpty()) return

        val lastLocation = savedMarkers.last()
        val lastLocationLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)

        for (location in savedMarkers) {
            val latLng = LatLng(location.latitude, location.longitude)
            val markerTitle = location.name
            val marker = gMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
            if (marker != null) {
                marker.tag = location
            }
        }


        gMap.setOnMarkerClickListener { marker ->
            val clickedLocation = marker.tag as? LocationData
            if (clickedLocation != null) {
                val intent = Intent(this, LocationActivity::class.java).apply {
                    putExtra("clickedLocation", clickedLocation)
                }
                startActivity(intent)
            }
            true
        }
    }

    private fun readMarkersList(callback: MarkerListCallback) {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("Markers")
        val markers: MutableList<LocationData> = mutableListOf()
        collectionRef.get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val location = document.toObject(LocationData::class.java)
                    markers.add(location)
                }
                callback.onMarkersReady(markers)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error getting data.", Toast.LENGTH_SHORT).show()
                callback.onMarkersReady(markers) // Return the empty list in case of failure
            }
    }
    override fun onMarkersReady(markers: MutableList<LocationData>) {
        savedMarkers = markers
        checkLocationPermission()
        showSavedMarkersOnMap()
    }
    private fun getAddressFromLocation(location: Location): String? {
        val geocoder = Geocoder(this)
        val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return addresses?.get(0)?.getAddressLine(0)
    }
}