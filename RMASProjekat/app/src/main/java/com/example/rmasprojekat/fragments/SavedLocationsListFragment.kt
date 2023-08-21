package com.example.rmasprojekat.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.MarkerListCallback
import com.example.rmasprojekat.databinding.FragmentSavedLocationsListBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class SavedLocationsListFragment : Fragment(), MarkerListCallback {

    private lateinit var lvSavedLocations: ListView
    private lateinit var btnFilter:FloatingActionButton
    private lateinit var savedMarkers: MutableList<LocationData>
    private lateinit var binding: FragmentSavedLocationsListBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSavedLocationsListBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Locations List"

        lvSavedLocations = view.findViewById(R.id.lv_WayPoints)
        btnFilter = view.findViewById(R.id.btnFilter)
        btnFilter.setOnClickListener(View.OnClickListener {
            currentLocation{
                if (it != null) {
                    showFilterDialog(it)
                }
            }
        })
        readMarkersList(this)
    }
    private fun fillList() {
        val markersList: MutableList<String> = mutableListOf()

        for (location in savedMarkers) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val formattedDate = dateFormat.format(location.timeCreated.toDate())
            markersList.add("${location.name}  Rating: ${location.avgRating}  Date: $formattedDate")
        }
        lvSavedLocations.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, markersList)
        lvSavedLocations.setOnItemClickListener { _, _, position, _ ->
            // Handle item click here
            val selectedLocation = savedMarkers[position]
            openLocationFragment(selectedLocation)
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
                Toast.makeText(requireContext(), "Error getting data.", Toast.LENGTH_SHORT).show()
                callback.onMarkersReady(markers) // Return the empty list in case of failure
            }
    }
    private fun openLocationFragment(location: LocationData) {
        val bundle = Bundle()
        bundle.putString("clickedLocationId", location.id) // Pass the location ID to LocationFragment
        findNavController().navigate(R.id.action_savedLocationsListFragment_to_locationFragment, bundle)
    }
    override fun onMarkersReady(markers: MutableList<LocationData>) {
        savedMarkers = markers
        fillList()
    }
    private fun showFilterDialog(currentLoc: LatLng) {
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
        // Create a filtered list based on the user's input
        val filteredList = savedMarkers.filter { location ->
            var authorMatch = location.author.contains(filterAuthor, ignoreCase = true)
            var typeMatch = location.type.contains(filterType, ignoreCase = true)
            val dateMatch = filterDate.isEmpty() || isDateInRange(location.timeCreated.toDate(), filterDate)

            if(filterAuthor=="")
                authorMatch=true
            if(filterType=="Any Type")
                typeMatch=true
            Log.d("Filter Values:","Author Matched: $authorMatch Type Matched: $typeMatch Date Matched: $dateMatch Radius: $selectedRadius")
            authorMatch && typeMatch && dateMatch&&calculateDistance(currentLoc.latitude,currentLoc.longitude,location.latitude,location.longitude)<selectedRadius
        }

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            filteredList.map {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                val formattedDate = dateFormat.format(it.timeCreated.toDate())
                "${it.name} Rating: ${it.avgRating} Date:$formattedDate"
            }
        )
        lvSavedLocations.adapter = adapter
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
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
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
        val a = Math.sin(dLat / 2)
            .pow(2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2).pow(2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return radiusOfEarth * c
    }
}