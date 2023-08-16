@file:Suppress("DEPRECATION")

package com.example.rmasprojekat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.rmasprojekat.data.LocationData
import com.example.rmasprojekat.data.MarkerListCallback
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), MarkerListCallback  {
    lateinit var auth: FirebaseAuth
    lateinit var user:FirebaseUser

    lateinit var tv_lat:TextView
    lateinit var tv_lon:TextView
    lateinit var tv_altitude:TextView
    lateinit var tv_accuracy:TextView
    lateinit var tv_speed:TextView
    lateinit var tv_sensor:TextView
    lateinit var tv_updates:TextView
    lateinit var tv_address:TextView
    lateinit var tv_waypointCount:TextView

    lateinit var sw_locationupdates:Switch
    lateinit var sw_gps:Switch

    lateinit var btn_newWaypoint:Button
    lateinit var btn_showWaypointList:Button
    lateinit var btn_showMap:Button

    lateinit var fusedLocationProviderClient:FusedLocationProviderClient
    lateinit var locationRequest:LocationRequest

    val DEFAULT_UPDATE_INTERVAL:Long=30000
    val FAST_UPDATE_INTERVAL:Long=5000
    val PERMISSIONS_FINE_LOCATION=99

    lateinit var currentLoc:Location
    lateinit var savedMarkers:MutableList<LocationData>
    private lateinit var locationCallback: LocationCallback
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth=FirebaseAuth.getInstance()
        user=auth.currentUser!!
        savedMarkers= mutableListOf()
        val drawerLayout = findViewById<DrawerLayout>(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this,
            drawerLayout,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        val navView = findViewById<NavigationView>(R.id.nav_view)
        navView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_item1 -> {
                    // Handle item 1 click
                    drawerLayout.closeDrawers()
                    true
                }
                R.id.logout -> {
                    // Handle logout click
                    auth.signOut()
                    val intent = Intent(this, Login::class.java)
                    startActivity(intent)
                    drawerLayout.closeDrawers()
                    true
                }
                // Handle more items as needed
                else -> false
            }

        }

        if(user==null){
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        tv_lat = findViewById(R.id.tv_lat)
        tv_lon = findViewById(R.id.tv_lon)
        tv_altitude = findViewById(R.id.tv_altitude)
        tv_accuracy = findViewById(R.id.tv_accuracy)
        tv_speed = findViewById(R.id.tv_speed)
        tv_sensor = findViewById(R.id.tv_sensor)
        tv_updates = findViewById(R.id.tv_updates)
        tv_address = findViewById(R.id.tv_address)
        tv_waypointCount = findViewById(R.id.tv_countOfCrumbs)

        sw_locationupdates = findViewById(R.id.sw_locationsupdates)
        sw_gps = findViewById(R.id.sw_gps)

        btn_newWaypoint=findViewById(R.id.btn_newWaypoint)
        btn_showWaypointList=findViewById(R.id.btn_showWaypointList)
        btn_showMap=findViewById(R.id.btn_showMap)

        locationRequest = LocationRequest.create().apply {
            interval = DEFAULT_UPDATE_INTERVAL // Specify the update interval in milliseconds
            fastestInterval = FAST_UPDATE_INTERVAL // Specify the fastest update interval in milliseconds
            priority = LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY // Specify the priority for location updates
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult ?: return
                for (location in locationResult.locations){
                    // Update UI with location data
                    updateUIValues(location)
                }
            }
        }
        btn_newWaypoint.setOnClickListener(View.OnClickListener {
            NewWaypoint()
        })
        btn_showWaypointList.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, ShowSavedLocationsList::class.java)
            startActivity(intent)
            finish()
        })
        btn_showMap.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
            finish()
        })
        sw_gps.setOnClickListener( View.OnClickListener {
            if(sw_gps.isChecked){
                locationRequest.priority=Priority.PRIORITY_HIGH_ACCURACY
                tv_sensor.text="Using GPS Sensors"
            }else{
                locationRequest.priority=Priority.PRIORITY_BALANCED_POWER_ACCURACY
                tv_sensor.text="Using Towers + WiFi"
            }
        })

        sw_locationupdates.setOnClickListener(View.OnClickListener {
            if(sw_locationupdates.isChecked){
                startLocationUpdates()
            }else{
                stopLocationUpdates()
            }
        })

        readMarkersList(this)
        updateGps()

    }//end of onCreate method
    private fun NewWaypoint(){
        readMarkersList(this)
        val db = FirebaseFirestore.getInstance()
        val collectionRef = db.collection("Markers")
        val markerDetails = LocationData(
            name = "Example",
            latitude = currentLoc.latitude,
            longitude = currentLoc.longitude,
            address = getAddressFromLocation(currentLoc).toString(),
            type = "Example"
        )
        collectionRef.add(markerDetails)
        updateUIValues(currentLoc)
        Toast.makeText(this, "Marker added.", Toast.LENGTH_SHORT).show()
    }
    private fun stopLocationUpdates() {
        tv_updates.setText("Location is NOT being tracked")
        tv_lat.text="Not tracking location"
        tv_lon.text="Not tracking location"
        tv_speed.text="Not tracking location"
        tv_accuracy.text="Not tracking location"
        tv_address.text="Not tracking location"
        tv_altitude.text="Not tracking location"
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun startLocationUpdates() {
        tv_updates.setText("Location is being tracked")
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this,"This app requires the location permission to work.",Toast.LENGTH_SHORT).show()
            return
        }
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,null)
        updateGps()

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when(requestCode){
                PERMISSIONS_FINE_LOCATION->{if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                    updateGps()
                }else{
                    Toast.makeText(this,"This app requires the location permission to work.",Toast.LENGTH_SHORT).show()
                    finish()
                }
        }
        }
    }

    private fun updateGps(){
        readMarkersList(this)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            fusedLocationProviderClient.getLastLocation().addOnSuccessListener {
                updateUIValues(it)
                currentLoc=it
            }
        }else{
            if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
                requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_FINE_LOCATION)
            }
        }
    }

    private fun updateUIValues(location: Location) {
        tv_lat.text= location.latitude.toString()
        tv_lon.text=location.longitude.toString()
        tv_accuracy.text=location.accuracy.toString()
        if(location.hasAltitude()){
            tv_altitude.text=location.altitude.toString()
        }else{
            tv_altitude.text="Not Available."
        }
        if(location.hasSpeed()){
            tv_speed.text=location.speed.toString()
        }else{
            tv_speed.text="Not Available."
        }
        val geocoder: Geocoder=Geocoder(this)
        try{
            val addresses:List<Address>? = geocoder.getFromLocation(location.latitude,location.longitude,1)
            if (addresses != null) {
                tv_address.text=addresses.get(0).getAddressLine(0).toString()
            }
        }catch(e:Exception){
            tv_address.text="Not Available."
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
        tv_waypointCount.text = savedMarkers.size.toString()
    }
    private fun getAddressFromLocation(location: Location): String? {
        val geocoder = Geocoder(this)
        val addresses: List<Address>? = geocoder.getFromLocation(location.latitude, location.longitude, 1)
        return addresses?.get(0)?.getAddressLine(0)
    }
}