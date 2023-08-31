package com.example.rmasprojekat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.rmasprojekat.fragments.LocationFragment
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
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    // Check the ID of the current destination fragment
                    when (destination.id) {
                        R.id.locationFragment -> {
                            // Hide the BottomNavigationView when LocationFragment is displayed
                            bottomNavigationView.visibility = View.GONE
                        }
                        else -> {
                            // Show the BottomNavigationView for other fragments
                            bottomNavigationView.visibility = View.VISIBLE
                        }
                    }
                }
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
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.logout_menu, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        val currentFragment = navController.currentDestination?.id

        val shouldShowLogout = currentFragment != R.id.registerFragment &&
                currentFragment != R.id.loginFragment

        menu?.findItem(R.id.action_logout)?.isVisible = shouldShowLogout

        return super.onPrepareOptionsMenu(menu)
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val navController = findNavController(R.id.nav_host_fragment)
        return if (id == R.id.action_logout) {
            auth.signOut()
            navController.navigate(R.id.loginFragment)
            true
        } else super.onOptionsItemSelected(item)
    }
}