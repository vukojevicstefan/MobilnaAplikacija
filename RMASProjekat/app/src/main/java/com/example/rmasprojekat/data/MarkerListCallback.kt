package com.example.rmasprojekat.data

interface MarkerListCallback {
    fun onMarkersReady(markers: MutableList<LocationData>)
}