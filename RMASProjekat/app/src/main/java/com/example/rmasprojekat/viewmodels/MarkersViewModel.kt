package com.example.rmasprojekat.viewmodels

import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.LocationData

class MarkersViewModel : ViewModel() {
    private var markers: MutableList<LocationData> = mutableListOf()

    fun getMarkers(): List<LocationData> {
        return markers
    }

    fun setMarkers(newMarkers: List<LocationData>) {
        markers.clear()
        markers.addAll(newMarkers)
    }
}
