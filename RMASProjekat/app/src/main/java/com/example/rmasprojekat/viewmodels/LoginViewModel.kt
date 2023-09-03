@file:Suppress("UNCHECKED_CAST")

package com.example.rmasprojekat.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObject

class LoginViewModel(private val userViewModel: CurrentUserViewModel) : ViewModel() {

    // Initialize Firebase Authentication and Firestore instances
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    // Function for logging in a user with email and password
    fun loginUserWithEmailAndPassword(
        email: String,
        password: String,
        callback: (Boolean) -> Unit
    ) {
        // Use Firebase Authentication to sign in with email and password
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User login successful, retrieve user details from Firestore
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    // Fetch user details from Firestore
                    firestore.collection("Users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                // User document exists, extract user data
                                val userData = document.data
                                val currentUser = User(
                                    uid,
                                    email,
                                    userData?.get("username") as? String ?: "",
                                    userData?.get("firstName") as? String ?: "",
                                    userData?.get("lastName") as? String ?: "",
                                    userData?.get("phoneNumber") as? String ?: "",
                                    userData?.get("score") as? Int ?: 0,
                                    userData?.get("likedReviews") as? MutableList<String> ?: mutableListOf(),
                                    userData?.get("photoPath") as? String ?: ""
                                )
                                // Set the current user in the ViewModel
                                userViewModel.setCurrentUser(currentUser)
                                callback(true)
                            } else {
                                // Handle if the user document doesn't exist
                                callback(false)
                            }
                        }
                        .addOnFailureListener {
                            // Handle any errors while fetching user details
                            callback(false)
                        }
                } else {
                    // Handle login failure
                    callback(false)
                }
            }
    }

    // Function for retrieving the current user's data from Firestore
    fun getCurrentUserFromFirestore(currentUserId: String?, callback: (User?) -> Unit) {
        if (currentUserId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef: DocumentReference = db.collection("Users").document(currentUserId)

            try {
                userRef.get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            // User document exists, convert it to a User object
                            val user = snapshot.toObject<User>()
                            callback(user)
                        } else {
                            callback(null) // User document does not exist
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.d("LoginViewModel getCurrentUserFromFirestore Error", e.message.toString())
                        callback(null) // Error occurred
                    }
            } catch (e: FirebaseFirestoreException) {
                Log.d("LoginViewModel getCurrentUserFromFirestore Error", e.message.toString())
                callback(null) // Error occurred
            }
        } else {
            Log.d("LoginViewModel getCurrentUserFromFirestore Error", "User is not authenticated")
            callback(null) // User is not authenticated
        }
    }
}