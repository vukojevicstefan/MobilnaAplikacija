@file:Suppress("DEPRECATION")

package com.example.rmasprojekat.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.databinding.FragmentMapBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class MapFragment : Fragment(), OnMapReadyCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var binding: FragmentMapBinding
    private lateinit var gMap: GoogleMap
    private lateinit var savedMarkers: MutableList<LocationData>
    private lateinit var auth: FirebaseAuth

    private lateinit var addMarker: FloatingActionButton
    private lateinit var btnFilter: FloatingActionButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Map"

        savedMarkers = mutableListOf()
        addMarker = view.findViewById(R.id.addMarker)
        btnFilter = view.findViewById(R.id.btnFilter)
        auth = FirebaseAuth.getInstance()

        val mapFragment =
            childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        readMarkersList()

        addMarker.setOnClickListener {
            newWaypoint()
        }
        btnFilter.setOnClickListener {
            currentLocation {
                if (it != null) {
                    showFilterDialog(it)
                }
                else{
                    Toast.makeText(requireContext(),"Error getting your location",Toast.LENGTH_SHORT).show()
                }
            }
        }
    }//end of onViewCreated
    private fun newWaypoint() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val dialogView = LayoutInflater.from(requireContext())
                        .inflate(R.layout.marker_dialog, null)
                    val nameEditText = dialogView.findViewById<EditText>(R.id.marker_name_edittext)
                    val typeSpinner = dialogView.findViewById<Spinner>(R.id.marker_type_spinner)

                    val markerTypes = arrayOf("Restaurant", "Coffee Shop", "Fast Food", "Hotel", "Park", "Gas Station", "Other")
                    val typeAdapter =
                        ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, markerTypes)
                    typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
                    typeSpinner.adapter = typeAdapter

                    AlertDialog.Builder(requireContext())
                        .setTitle("Add Marker")
                        .setView(dialogView)
                        .setPositiveButton("Add") { _, _ ->
                            val name = nameEditText.text.toString()
                            val type = markerTypes[typeSpinner.selectedItemPosition]

                            val address = getAddressFromLocation(location).toString()

                            val currentTime = Timestamp.now()
                            returnUsername { user ->
                                if (user.isNotEmpty()) {
                                    val markerDetails = LocationData(id = "", name = name, latitude = location.latitude, longitude = location.longitude, address = address, type = type, photos = mutableListOf(), reviews = mutableListOf(), avgRating = 0.0, reviewCount = 0, timeCreated = currentTime, author = user
                                    )

                                    val db = FirebaseFirestore.getInstance()
                                    val collectionRef = db.collection("Markers")
                                    // Add the marker details to Firestore and get the generated document reference
                                    collectionRef.add(markerDetails)
                                        .addOnSuccessListener { documentReference ->
                                            val data = hashMapOf("id" to documentReference.id)
                                            markerDetails.id = documentReference.id
                                            db.collection("Markers").document(documentReference.id)
                                                .set(data, SetOptions.merge())
                                                .addOnSuccessListener {
                                                    readMarkersList()
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Marker added.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                                .addOnFailureListener { e ->
                                                    // Handle Firestore write failure
                                                    Toast.makeText(
                                                        requireContext(),
                                                        "Failed to add marker: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            // Handle Firestore add failure
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to add marker: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                } else {
                                    // Handle the case where userId is empty
                                    Toast.makeText(
                                        requireContext(),
                                        "User ID is empty.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                } else {
                    // Handle the case where location is null
                    Toast.makeText(
                        requireContext(),
                        "Location not available.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }//End of newWaypoint()
    override fun onMapReady(googleMap: GoogleMap) {
        gMap = googleMap
        checkLocationPermission()
        showSavedMarkersOnMap()
    }
    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            gMap.isMyLocationEnabled = true
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())
            fusedLocationClient.lastLocation.addOnSuccessListener(
                requireActivity()
            ) { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val userLocation = LatLng(latitude, longitude)
                    gMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 18f))
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            showUserLocation()
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            }
        }
    }
    private fun showSavedMarkersOnMap() {
        gMap.clear()

        if (savedMarkers.isEmpty()) return

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

                val args = Bundle()
                args.putString("clickedLocationId", clickedLocation.id)
                findNavController().navigate(R.id.action_mapFragment_to_locationFragment, args)
            }
            true
        }

    }
    private fun readMarkersList() {
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("Markers")
        collectionRef.get()
            .addOnSuccessListener { documents ->
                val markers: MutableList<LocationData> = mutableListOf()
                for (document in documents) {
                    val location = document.toObject(LocationData::class.java)
                    markers.add(location)
                }
                onMarkersReady(markers)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error getting data.", Toast.LENGTH_SHORT).show()
                onMarkersReady(emptyList()) // Return an empty list in case of failure
            }
    }
    private fun onMarkersReady(markers: List<LocationData>) {
        savedMarkers = markers.toMutableList()

            checkLocationPermission()
            showSavedMarkersOnMap()
    }
    private fun getAddressFromLocation(location: Location): String? {
        val geocoder = Geocoder(requireContext())
        val addresses: List<Address>? =
            geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return addresses?.get(0)?.getAddressLine(0)
    }
    private fun returnUsername(callback: (String) -> Unit) {
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
                            callback(u) // Pass the retrieved id to the callback
                            return@addOnSuccessListener
                        }
                    }
                    callback("") // If no username is found, pass the default value
                }
                .addOnFailureListener {
                    callback("") // Handle failure by passing the default value
                }
        } else {
            callback("") // If the user is null, pass the default value
        }
    }
    private fun showFilterDialog(currentLoc:LatLng) {
        readMarkersList()
        val dialogView = layoutInflater.inflate(R.layout.filter_dialog, null)
        val authorEditText = dialogView.findViewById<EditText>(R.id.filter_marker_author_edittext)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.filter_marker_type_spinner)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_date)
        val btnChange=dialogView.findViewById<Button>(R.id.btnChangeTime)
        val numberPicker = dialogView.findViewById<SeekBar>(R.id.filter_marker_radius_seekbar)
        val selectedRadiusTextView = dialogView.findViewById<TextView>(R.id.selected_radius_text)

        numberPicker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                // Update a TextView to display the selected radius
                selectedRadiusTextView.text = "Radius: $progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        btnChange
            .setOnClickListener {
                val dateRangePicker =
                    MaterialDatePicker.Builder.dateRangePicker()
                        .setTitleText("Kreirano")
                        .setSelection(
                            androidx.core.util.Pair(
                                MaterialDatePicker.thisMonthInUtcMilliseconds(),
                                MaterialDatePicker.todayInUtcMilliseconds()
                            )
                        )
                        .build()
                dateRangePicker.addOnPositiveButtonClickListener {
                    tvDate.text = getDateString(it.first, it.second)
                }
                dateRangePicker
                    .show(requireActivity().supportFragmentManager, "tag")
            }
        val markerTypes = arrayOf("Any Type", "Restaurant", "Coffee Shop",
            "Fast Food",
            "Hotel",
            "Park",
            "Gas Station",
            "Other"
        )
        val typeAdapter =
            ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, markerTypes)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Filter Locations")
            .setPositiveButton("Filter"){_,_->
                val author= authorEditText.text.toString()
                val type = markerTypes[typeSpinner.selectedItemPosition]
                val date=tvDate.text.toString()
                val selectedRadius = numberPicker.progress
                applyFilters(author,type,date,selectedRadius,currentLoc)
            }.setNegativeButton("Cancel",null)

        val dialog = dialogBuilder.create()
        dialog.show()

    }
    private fun applyFilters(filterAuthor: String, filterType: String, filterDate: String, selectedRadius:Int,currentLoc: LatLng) {

        val filteredList = savedMarkers.filter { location ->
            var authorMatch = location.author.contains(filterAuthor, ignoreCase = true)
            var typeMatch = location.type.contains(filterType, ignoreCase = true)
            val dateMatch = filterDate.isEmpty() || isDateInRange(location.timeCreated.toDate(), filterDate)

            if(filterAuthor=="")
                authorMatch=true
            if(filterType=="Any Type")
                typeMatch=true
            Log.d("Filter Values:","Author Matched: $authorMatch Type Matched: $typeMatch Date Matched: $dateMatch")
            authorMatch && typeMatch && dateMatch&&calculateDistance(currentLoc.latitude,currentLoc.longitude,location.latitude,location.longitude)<selectedRadius
        }

        savedMarkers=filteredList.toMutableList()
        showSavedMarkersOnMap()
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

    private fun getDateString(start: Long, end: Long): String {
        val startStr = DateFormat
            .getDateInstance()
            .format(Date(start))
        val endStr = DateFormat
            .getDateInstance()
            .format(Date(end))
        return "$startStr - $endStr"
    }
    private fun currentLocation(callback: (LatLng?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient =
                LocationServices.getFusedLocationProviderClient(requireContext())

            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    // Create a LatLng object and pass it to the callback
                    val currentLatLng = LatLng(latitude, longitude)
                    Log.d("Current Location", "Latitude: $latitude, Longitude: $longitude")
                    callback(currentLatLng)
                } else {
                    // Handle the case where location is null
                    Log.d("Current Location", "Location not available.")
                    callback(null)
                }
            }
        } else {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            callback(null)
        }
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
        val a = sin(dLat / 2).pow(2) + cos(lat1Rad) * cos(lat2Rad) * sin(dLon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return radiusOfEarth * c
    }
}