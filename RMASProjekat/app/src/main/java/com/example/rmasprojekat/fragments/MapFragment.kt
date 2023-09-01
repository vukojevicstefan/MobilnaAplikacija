@file:Suppress("DEPRECATION")

package com.example.rmasprojekat.fragments

import android.Manifest
import android.content.pm.PackageManager
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
import com.example.rmasprojekat.MainActivity
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.databinding.FragmentMapBinding
import com.example.rmasprojekat.viewmodels.MapViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import java.text.DateFormat
import java.util.Date

class MapFragment : Fragment(), OnMapReadyCallback {

    private val LOCATION_PERMISSION_REQUEST_CODE = 1
    private lateinit var binding: FragmentMapBinding
    private lateinit var gMap: GoogleMap
    private lateinit var savedMarkers: MutableList<LocationData>
    private lateinit var auth: FirebaseAuth

    private lateinit var addMarker: FloatingActionButton
    private lateinit var btnFilter: FloatingActionButton

    private lateinit var viewModel: MapViewModel
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

        val activity = requireActivity() as MainActivity
        viewModel = MapViewModel(activity.userViewModel, activity.markersViewModel, activity.filteredMarkersViewModel)
        addMarker = view.findViewById(R.id.addMarker)
        btnFilter = view.findViewById(R.id.btnFilter)
        auth = FirebaseAuth.getInstance()
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment

        mapFragment.getMapAsync(this)

        viewModel.readMarkersList(requireContext())
        savedMarkers = viewModel.getMarkers().toMutableList()
        addMarker.setOnClickListener {
            viewModel.newWaypoint(requireContext(),requireActivity())

            savedMarkers = viewModel.getMarkers().toMutableList()
            showSavedMarkersOnMap()
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
    private fun showFilterDialog(currentLoc:LatLng) {
        savedMarkers=viewModel.getMarkers().toMutableList()
        showSavedMarkersOnMap()
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
                viewModel.applyFilters(author,type,date,selectedRadius,currentLoc)
                savedMarkers=viewModel.getFilteredMarkers().toMutableList()
                showSavedMarkersOnMap()
            }.setNegativeButton("Cancel",null)

        val dialog = dialogBuilder.create()
        dialog.show()
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
}