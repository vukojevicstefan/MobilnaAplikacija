@file:Suppress("DEPRECATION")

package com.example.rmasprojekat

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.MarkerListCallback
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.firestore.FirebaseFirestore

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, MarkerListCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var gMap: GoogleMap
    private lateinit var map: FrameLayout
    private lateinit var savedMarkers: MutableList<LocationData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        map = findViewById(R.id.map)
        savedMarkers = mutableListOf()
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        readMarkersList(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        checkLocationPermission()
        showsavedMarkersOnMap()
    }

    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            gMap.isMyLocationEnabled = true
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationClient.lastLocation.addOnSuccessListener(this, object : OnSuccessListener<Location?> {
                override fun onSuccess(location: Location?) {
                    if (location != null) {
                        val latitude = location.latitude
                        val longitude = location.longitude
                        val userLocation = LatLng(latitude, longitude)
                        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 16f))
                    }
                }
            })
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

    private fun showsavedMarkersOnMap() {
        if (savedMarkers.isEmpty()) return

        val lastLocation = savedMarkers.last()
        val lastLocationLatLng = LatLng(lastLocation.latitude, lastLocation.longitude)

        for (location in savedMarkers) {
            val latLng = LatLng(location.latitude, location.longitude)
            val markerTitle = location.name
            gMap.addMarker(MarkerOptions().position(latLng).title(markerTitle))
        }

        gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocationLatLng, 16f))

        gMap.setOnMarkerClickListener { marker ->
            var clicks = marker.tag as? Int ?: 0
            clicks++
            marker.tag = clicks
            Toast.makeText(this, "Marker was clicked $clicks times", Toast.LENGTH_SHORT).show()
            false
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
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error getting data.", Toast.LENGTH_SHORT).show()
                callback.onMarkersReady(markers) // Return the empty list in case of failure
            }
    }
    override fun onMarkersReady(markers: MutableList<LocationData>) {
        savedMarkers = markers
        checkLocationPermission()
        showsavedMarkersOnMap()
    }
}