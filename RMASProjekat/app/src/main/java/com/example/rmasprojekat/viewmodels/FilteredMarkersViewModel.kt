package com.example.rmasprojekat.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.LocationData

class FilteredMarkersViewModel : ViewModel() {
    private val _markers = MutableLiveData<List<LocationData>>()
    val markers: LiveData<List<LocationData>> = _markers
    init {
        _markers.value = emptyList()
    }
    fun getMarkers(): List<LocationData>? {
        return _markers.value
    }
    fun setMarkers(newMarkers: List<LocationData>) {
        _markers.value = newMarkers
    }
}
