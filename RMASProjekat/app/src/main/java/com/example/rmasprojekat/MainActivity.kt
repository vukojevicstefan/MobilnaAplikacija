package com.example.rmasprojekat

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navController = findNavController(R.id.nav_host_fragment)

        auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            bottomNavigationView = findViewById(R.id.bottom_navigation)
            hideBottomNavigationView()
            if (user == null) {
                navController.navigate(R.id.loginFragment)
            } else {
                showBottomNavigationView()
                navController.navigate(R.id.mapFragment)

                // Handle BottomNavigationView item clicks
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        R.id.navMapsActivity -> {
                            navController.navigate(R.id.mapFragment)
                            true
                        }
                        R.id.navMarkersList -> {
                            navController.navigate(R.id.savedLocationsListFragment)
                            true
                        }
                        R.id.navLeaderboard -> {
                            navController.navigate(R.id.leaderboardFragment)
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }
    private fun hideBottomNavigationView() {
        bottomNavigationView.visibility = View.GONE
    }

    private fun showBottomNavigationView() {
        bottomNavigationView.visibility = View.VISIBLE
    }
    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }
    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }
}

