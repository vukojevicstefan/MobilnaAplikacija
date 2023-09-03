@file:Suppress("DEPRECATION")

package  com.example.rmasprojekat

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

    // Declare some class-level variables
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var auth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener

    // Initialize view models for managing data
    val userViewModel: CurrentUserViewModel by viewModels()
    val markersViewModel: MarkersViewModel by viewModels()
    val filteredMarkersViewModel: FilteredMarkersViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Find the navigation controller associated with this activity
        val navController = findNavController(R.id.nav_host_fragment)

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Define an AuthStateListener to handle changes in user authentication state
        authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            bottomNavigationView = findViewById(R.id.bottom_navigation)

            // Check if a user is authenticated
            if (user == null) {
                // If not authenticated, navigate to the login fragment and hide the bottom navigation
                navController.navigate(R.id.loginFragment)
                bottomNavigationView.visibility = View.GONE
            } else {
                // If authenticated, set up navigation based on the current fragment
                navController.addOnDestinationChangedListener { _, destination, _ ->
                    invalidateOptionsMenu()
                    when (destination.id) {
                        // Hide bottom navigation for certain fragments
                        R.id.locationFragment -> {
                            bottomNavigationView.visibility = View.GONE
                        }
                        R.id.cameraFragment -> {
                            bottomNavigationView.visibility = View.GONE
                        }
                        R.id.loginFragment -> {
                            bottomNavigationView.visibility = View.GONE
                        }
                        R.id.registerFragment -> {
                            bottomNavigationView.visibility = View.GONE
                        }
                        else -> {
                            // Show bottom navigation for other fragments
                            bottomNavigationView.visibility = View.VISIBLE
                        }
                    }
                }

                // Navigate to the map fragment by default
                navController.navigate(R.id.mapFragment)

                // Handle BottomNavigationView item clicks
                bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                    when (item.itemId) {
                        // Navigate to the map fragment
                        R.id.navMapsActivity -> {
                            if (navController.currentDestination!!.id != R.id.mapFragment)
                                navController.navigate(R.id.mapFragment)
                            true
                        }
                        // Navigate to the saved locations list fragment
                        R.id.navMarkersList -> {
                            if (navController.currentDestination!!.id != R.id.savedLocationsListFragment)
                                navController.navigate(R.id.savedLocationsListFragment)
                            true
                        }
                        // Navigate to the leaderboard fragment
                        R.id.navLeaderboard -> {
                            if (navController.currentDestination!!.id != R.id.leaderboardFragment)
                                navController.navigate(R.id.leaderboardFragment)
                            true
                        }
                        else -> false
                    }
                }
            }
        }
    }

    // Start listening for changes in authentication state when the activity starts
    override fun onStart() {
        super.onStart()
        auth.addAuthStateListener(authStateListener)
    }

    // Stop listening for changes in authentication state when the activity stops
    override fun onStop() {
        super.onStop()
        auth.removeAuthStateListener(authStateListener)
    }

    // Inflate the options menu for the activity
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.logout_menu, menu)
        return true
    }

    // Update the options menu based on the current fragment
    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        val currentFragment = navController.currentDestination?.id

        // Determine whether to show the logout option in the menu
        val shouldShowLogout = currentFragment != R.id.registerFragment &&
                currentFragment != R.id.loginFragment

        menu?.findItem(R.id.action_logout)?.isVisible = shouldShowLogout

        return super.onPrepareOptionsMenu(menu)
    }

    // Handle menu item clicks, specifically the logout action
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        val navController = findNavController(R.id.nav_host_fragment)
        return if (id == R.id.action_logout) {
            // Sign out the user, hide the logout option, and navigate to the login fragment
            auth.signOut()
            item.isVisible = false
            navController.navigate(R.id.loginFragment)
            true
        } else super.onOptionsItemSelected(item)
    }
}