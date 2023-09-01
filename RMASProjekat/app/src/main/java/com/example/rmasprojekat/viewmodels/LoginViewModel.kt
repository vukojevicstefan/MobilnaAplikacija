package com.example.rmasprojekat.viewmodels

import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class LoginViewModel(private val userViewModel: CurrentUserViewModel) : ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun loginUserWithEmailAndPassword(
        email: String,
        password: String,
        callback: (Boolean) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    val uid = user?.uid ?: ""

                    // Fetch user details from Firestore
                    firestore.collection("Users")
                        .document(uid)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
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
    fun getCurrentUserFromFirestore(currentUserId:String?, callback: (User?) -> Unit) {
        if (currentUserId != null) {
            val db = FirebaseFirestore.getInstance()
            val userRef: DocumentReference = db.collection("Users").document(currentUserId)

            try {
                userRef.get()
                    .addOnSuccessListener { snapshot ->
                        if (snapshot.exists()) {
                            val user = snapshot.toObject<User>()
                            callback(user)
                        } else {
                            callback(null) // User document does not exist
                        }
                    }
                    .addOnFailureListener { e ->
                        callback(null) // Error occurred
                    }
            } catch (e: FirebaseFirestoreException) {
                callback(null) // Error occurred
            }
        } else {
            callback(null) // User is not authenticated
        }
    }
}