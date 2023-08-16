package com.example.rmasprojekat

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.MarkerListCallback
import com.google.firebase.firestore.FirebaseFirestore

class ShowSavedLocationsList : AppCompatActivity(), MarkerListCallback {

    lateinit var lv_savedLocations: ListView
    lateinit var savedMarkers: MutableList<LocationData>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_saved_locations_list)
        lv_savedLocations = findViewById(R.id.lv_WayPoints)

        readMarkersList(this)
    }

    private fun fillList() {
        var markersList: MutableList<String> = mutableListOf()

        for (location in savedMarkers) {
            markersList.add(location.name)
        }
        lv_savedLocations.adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, markersList)
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

        for (marker in savedMarkers) {
            Toast.makeText(this, marker.address, Toast.LENGTH_SHORT).show()
        }

        fillList()
    }
}