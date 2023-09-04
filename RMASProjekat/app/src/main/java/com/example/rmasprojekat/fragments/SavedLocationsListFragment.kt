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
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rmasprojekat.MainActivity
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.databinding.FragmentSavedLocationsListBinding
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.pow

class SavedLocationsListFragment : Fragment() {

    // Initialize UI elements and variables
    private lateinit var lvSavedLocations: ListView
    private lateinit var btnFilter: FloatingActionButton
    private lateinit var binding: FragmentSavedLocationsListBinding
    private lateinit var savedMarkers: MutableList<LocationData>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the fragment layout using data binding
        binding = FragmentSavedLocationsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the action bar title
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Locations List"

        // Get a reference to the MainActivity
        val activity = requireActivity() as MainActivity

        // Initialize UI elements
        lvSavedLocations = view.findViewById(R.id.lv_WayPoints)
        btnFilter = view.findViewById(R.id.btnFilter)

        // Set a click listener for the filter button
        btnFilter.setOnClickListener {
            fillList(savedMarkers)
            // Get the current location and show a filter dialog when the button is clicked
            currentLocation { currentLoc ->
                if (currentLoc != null) {
                    showFilterDialog(currentLoc)
                }
            }
        }

        // Observe the markers LiveData and update the UI when it changes
        activity.markersViewModel.markers.observe(viewLifecycleOwner) { markers ->
            // Sort the markers by average rating in descending order
            val sortedMarkers = sortMarkers(markers)
            savedMarkers = markers.toMutableList()
            // Fill the list view with the sorted markers
            fillList(sortedMarkers)
        }
    }

    // Function to sort markers by average rating in descending order
    private fun sortMarkers(markers: List<LocationData>): List<LocationData> {
        return markers.sortedByDescending { it.avgRating }
    }

    // Function to populate the list view with location data
    private fun fillList(savedMarkers: List<LocationData>) {
        val markersList: MutableList<String> = mutableListOf()
        // Format location data and add it to the list
        for (location in savedMarkers) {
            val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
            val formattedDate = dateFormat.format(location.timeCreated.toDate())
            markersList.add("${location.name} ${location.type} ${location.author} Rating: ${location.avgRating}  Date: $formattedDate")
        }

        // Create an ArrayAdapter to display the list in the ListView
        lvSavedLocations.adapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, markersList)

        // Set an item click listener for the list view
        lvSavedLocations.setOnItemClickListener { _, _, position, _ ->
            // Handle item click by opening the LocationFragment
            val selectedLocation = savedMarkers[position]
            openLocationFragment(selectedLocation)
        }
    }

    // Function to open the LocationFragment with the selected location data
    private fun openLocationFragment(location: LocationData) {
        val bundle = Bundle()
        bundle.putString("clickedLocationId", location.id) // Pass the location ID to LocationFragment
        findNavController().navigate(R.id.action_savedLocationsListFragment_to_locationFragment, bundle)
    }

    // Function to show a filter dialog for location filtering
    private fun showFilterDialog(currentLoc: LatLng) {
        val dialogView = layoutInflater.inflate(R.layout.filter_dialog, null)

        // Initialize UI elements in the filter dialog
        val authorEditText = dialogView.findViewById<EditText>(R.id.filter_marker_author_edittext)
        val typeSpinner = dialogView.findViewById<Spinner>(R.id.filter_marker_type_spinner)
        val tvDate = dialogView.findViewById<TextView>(R.id.tv_date)
        val btnChange = dialogView.findViewById<Button>(R.id.btnChangeTime)
        val numberPicker = dialogView.findViewById<SeekBar>(R.id.filter_marker_radius_seekbar)
        val selectedRadiusTextView = dialogView.findViewById<TextView>(R.id.selected_radius_text)

        // Set a seek bar change listener to display the selected radius
        numberPicker.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                selectedRadiusTextView.text = "Radius: $progress km"
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })

        // Set a click listener for the date range picker button
        btnChange.setOnClickListener {
            val dateRangePicker = MaterialDatePicker.Builder.dateRangePicker()
                .setTitleText("Kreirano")
                .setSelection(
                    androidx.core.util.Pair(
                        MaterialDatePicker.thisMonthInUtcMilliseconds(),
                        MaterialDatePicker.todayInUtcMilliseconds()
                    )
                )
                .build()

            // Add a positive button click listener for date selection
            dateRangePicker.addOnPositiveButtonClickListener {
                tvDate.text = getDateString(it.first, it.second)
            }

            // Show the date range picker dialog
            dateRangePicker.show(requireActivity().supportFragmentManager, "tag")
        }

        // Initialize the marker type spinner with options
        val markerTypes = arrayOf(
            "Any Type", "Restaurant", "Coffee Shop",
            "Fast Food", "Hotel", "Park", "Gas Station", "Other"
        )
        val typeAdapter = ArrayAdapter(requireContext(), R.layout.spinner_dropdown_item, markerTypes)
        typeAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        typeSpinner.adapter = typeAdapter

        // Create and show the filter dialog
        val dialogBuilder = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setTitle("Filter Locations")
            .setPositiveButton("Filter") { _, _ ->
                val author = authorEditText.text.toString()
                val type = markerTypes[typeSpinner.selectedItemPosition]
                val date = tvDate.text.toString()
                val selectedRadius = numberPicker.progress
                // Apply the selected filters
                applyFilters(author, type, date, selectedRadius, currentLoc)
            }
            .setNegativeButton("Cancel", null)

        val dialog = dialogBuilder.create()
        dialog.show()
    }

    // Function to apply filters to the list of locations
    private fun applyFilters(
        filterAuthor: String,
        filterType: String,
        filterDate: String,
        selectedRadius: Int,
        currentLoc: LatLng
    ) {
        // Create a filtered list based on the user's input
        val filteredList = savedMarkers.filter { location ->
            var authorMatch = location.author.contains(filterAuthor, ignoreCase = true)
            var typeMatch = location.type.contains(filterType, ignoreCase = true)
            val dateMatch = filterDate.isEmpty() || isDateInRange(location.timeCreated.toDate(), filterDate)

            // If the filterAuthor is empty, consider it a match
            if (filterAuthor.isEmpty()) {
                authorMatch = true
            }
            // If the filterType is "Any Type," consider it a match
            if (filterType == "Any Type") {
                typeMatch = true
            }

            // Calculate the distance between the current location and the location in the list
            val distance = calculateDistance(currentLoc.latitude, currentLoc.longitude, location.latitude, location.longitude)

            // Log filter values for debugging
            Log.d("Filter Values:", "Author Matched: $authorMatch Type Matched: $typeMatch Date Matched: $dateMatch Radius: $selectedRadius Distance: $distance")

            // Return true if all filter conditions are met
            authorMatch && typeMatch && dateMatch && (distance <= selectedRadius)
        }

        // Update the ListView with the filtered list
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_list_item_1,
            filteredList.map {
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
                val formattedDate = dateFormat.format(it.timeCreated.toDate())
                "${it.name} Rating: ${it.avgRating} Date: $formattedDate"
            }
        )
        lvSavedLocations.adapter = adapter
    }

    // Function to check if a date is within a specified range
    private fun isDateInRange(locationDate: Date, filterDate: String): Boolean {
        val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.US)
        val filterDateParts = filterDate.split(" - ")

        try {
            if (filterDateParts.size == 2) {
                val startDate = dateFormat.parse(filterDateParts[0])
                val endDate = dateFormat.parse(filterDateParts[1])

                if (startDate != null && endDate != null) {
                    return locationDate >= startDate && locationDate <= endDate
                }
            }
        } catch (e: ParseException) {
            Log.d("ParseException", e.message.toString())
        }
        return false
    }

    // Function to get a formatted date string from timestamp values
    private fun getDateString(start: Long, end: Long): String {
        val startStr = DateFormat.getDateInstance().format(Date(start))
        val endStr = DateFormat.getDateInstance().format(Date(end))
        return "$startStr - $endStr"
    }

    // Function to get the current device location
    private fun currentLocation(callback: (LatLng?) -> Unit) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

            // Get the last known location and pass it to the callback
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latitude = location.latitude
                    val longitude = location.longitude
                    val currentLatLng = LatLng(latitude, longitude)
                    Log.d("Current Location", "Latitude: $latitude, Longitude: $longitude")
                    callback(currentLatLng)
                } else {
                    Log.d("Current Location", "Location not available.")
                    callback(null)
                }
            }
        } else {
            // Request location permission if it's not granted
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            callback(null)
        }
    }

    // Function to calculate the distance between two sets of coordinates
    private fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val radiusOfEarth = 6371 // Earth's radius in kilometers

        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula to calculate distance
        val dLat = lat2Rad - lat1Rad
        val dLon = lon2Rad - lon1Rad
        val a = Math.sin(dLat / 2).pow(2) + Math.cos(lat1Rad) * Math.cos(lat2Rad) * Math.sin(dLon / 2).pow(2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return radiusOfEarth * c
    }
}