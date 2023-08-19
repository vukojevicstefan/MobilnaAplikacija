package com.example.rmasprojekat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {

    private lateinit var editTextUsername: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextFirstName: TextInputEditText
    private lateinit var editTextLastName: TextInputEditText
    private lateinit var editTextPhoneNumber: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextConfirmPassword:TextInputEditText
    private lateinit var buttonReg: Button
    private lateinit var auth :FirebaseAuth
    private lateinit var progressBar :ProgressBar
    private lateinit var goToLogIn: TextView

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth = FirebaseAuth.getInstance()

        editTextEmail = findViewById(R.id.email)
        editTextUsername = findViewById(R.id.username)
        editTextPassword = findViewById(R.id.password)
        editTextConfirmPassword=findViewById(R.id.confirmPassword)
        editTextFirstName = findViewById(R.id.first_name)
        editTextLastName = findViewById(R.id.last_name)
        editTextPhoneNumber = findViewById(R.id.phone_number)

        buttonReg = findViewById(R.id.btn_register)
        progressBar = findViewById(R.id.progressBar)
        goToLogIn = findViewById(R.id.loginNow)

        goToLogIn.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        buttonReg.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val email = editTextEmail.text.toString()
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()
            val secondPassword=editTextConfirmPassword.text.toString()
            val firstName = editTextFirstName.text.toString()
            val lastName = editTextLastName.text.toString()
            val phoneNumber = editTextPhoneNumber.text.toString()
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(firstName)
                || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(phoneNumber)
            ) {
                Toast.makeText(this, "Please fill in all empty fields.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            } else {
                isUsernameTaken(username) { uTaken ->
                    if (uTaken) {
                        Toast.makeText(this, "Username is taken.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken
                    } else if (password.length < 6) {
                        Toast.makeText(this, "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken
                    } else if(password!=secondPassword){
                        Toast.makeText(this, "Passwords must match.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken

                    }
                    else {
                        auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener { task ->
                            progressBar.visibility = View.GONE
                            if (task.isSuccessful) {
                                val user = auth.currentUser
                                val uid = user?.uid ?: ""

                                // Update user profile with display name
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setDisplayName(username)
                                    .build()

                                user?.updateProfile(profileUpdates)
                                    ?.addOnCompleteListener { profileUpdateTask ->
                                        if (profileUpdateTask.isSuccessful) {
                                            // Registration successful and profile updated
                                            val db = FirebaseFirestore.getInstance()
                                            val userRef = db.collection("Users").document(uid)

                                            val userDetails = hashMapOf(
                                                "id" to "",
                                                "email" to email,
                                                "username" to username,
                                                "first name" to firstName,
                                                "last name" to lastName,
                                                "password" to password,
                                                "phone number" to phoneNumber,
                                                "score" to 0,
                                                "likedReviews" to mutableListOf<String>()
                                            )

                                            userRef.set(userDetails)
                                                .addOnSuccessListener {
                                                    // Additional user details saved successfully
                                                    Toast.makeText(this, "Registration successful.", Toast.LENGTH_SHORT).show()
                                                    val intent = Intent(this, Login::class.java)
                                                    startActivity(intent)
                                                    finish()
                                                }
                                                .addOnFailureListener {
                                                    // Error saving additional user details
                                                    Toast.makeText(this, "Error saving user details.", Toast.LENGTH_SHORT).show()
                                                }
                                        } else {
                                            // Profile update failed
                                            Toast.makeText(this, "Profile update failed.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                // If sign-in fails, display a message to the user.
                                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }

        }
    }
    private fun isUsernameTaken(username: String, callback: (Boolean) -> Unit) {
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