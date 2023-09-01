package com.example.rmasprojekat.viewmodels

import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class RegisterViewModel(private val userViewModel: CurrentUserViewModel) : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

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
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""
                    val db = FirebaseFirestore.getInstance()
                    val userRef = db.collection("Users").document(uid)

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

                    userRef.set(userDetails)
                        .addOnSuccessListener {
                            val currentUser = User(uid, email,username,firstName,lastName,phoneNumber,0,mutableListOf<String>(),imageUri)
                            userViewModel.setCurrentUser(currentUser)
                            callback(true)
                        }
                        .addOnFailureListener {
                            callback(false)
                        }
                } else
                    callback(false)
            }
    }
    fun isUsernameTaken(username: String, callback: (Boolean) -> Unit) {
        val db = FirebaseFirestore.getInstance()

        db.collection("Users")
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty) // Pass true if documents exist, indicating username is taken
            }
            .addOnFailureListener {
                callback(false) // Pass false if there's a failure
            }
    }
}

