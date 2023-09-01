@file:Suppress("DEPRECATION")

package com.example.rmasprojekat

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.example.rmasprojekat.viewmodels.CurrentUserViewModel
import com.example.rmasprojekat.viewmodels.FilteredMarkersViewModel
import com.example.rmasprojekat.viewmodels.MarkersViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth


class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    val userViewModel: CurrentUserViewModel by viewModels()
    val markersViewModel: MarkersViewModel by viewModels()
    val filteredMarkersViewModel: FilteredMarkersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val navController = findNavController(R.id.nav_host_fragment)

        auth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            bottomNavigationView = findViewById(R.id.bottom_navigation)
            if (user == null) {
                navController.navigate(R.id.loginFragment)
                bottomNavigationView.visibility = View.GONE
            } else {
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    invalidateOptionsMenu()
                    when (destination.id) {
                        R.id.locationFragment -> {
                            bottomNavigationView.visibility = View.GONE
                        }
                        R.id.cameraFragment->{
                            bottomNavigationView.visibility = View.GONE
                        }
                        R.id.loginFragment->{
                            bottomNavigationView.visibility = View.GONE
                        }
                        R.id.registerFragment->{
                            bottomNavigationView.visibility = View.GONE
                        }
                        else -> {
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
            item.isVisible=false
            navController.navigate(R.id.loginFragment)
            true
        } else super.onOptionsItemSelected(item)
    }
}