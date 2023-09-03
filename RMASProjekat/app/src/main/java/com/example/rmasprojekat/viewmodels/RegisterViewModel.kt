package com.example.rmasprojekat.viewmodels

import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterViewModel(private val userViewModel: CurrentUserViewModel) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // This function is used to create a new user with email and password.
    fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        username: String,
        firstName: String,
        lastName: String,
        phoneNumber: String,
        imageUri: String,
        callback: (Boolean) -> Unit,
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // User creation was successful
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection("Users").document(uid)

                    // Create a map of user details to be stored in Firestore
                    val userDetails = hashMapOf(
                        "id" to uid,
                        "email" to email,
                        "username" to username,
                        "first name" to firstName,
                        "last name" to lastName,
                        "password" to password,
                        "phone number" to phoneNumber,
                        "score" to 0,
                        "likedReviews" to mutableListOf<String>(),
                        "photoPath" to imageUri
                    )

                    // Store user details in Firestore
                    userRef.set(userDetails)
                        .addOnSuccessListener {
                            // Successfully stored user details
                            val currentUser = User(uid, email, username, firstName, lastName, phoneNumber, 0, mutableListOf<String>(), imageUri)
                            userViewModel.setCurrentUser(currentUser)
                            callback(true)
                        }
                        .addOnFailureListener {
                            // Failed to store user details
                            callback(false)
                        }
                } else {
                    // User creation failed
                    callback(false)
                }
            }
    }
    // This function checks if a username is already taken in Firestore.
    fun isUsernameTaken(username: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                // Check if documents exist, indicating that the username is taken
                callback(!documents.isEmpty)
            }
            .addOnFailureListener {
                // Pass false if there's a failure
                callback(false)
            }
    }
}