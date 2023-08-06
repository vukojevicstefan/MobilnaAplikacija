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
import com.google.firebase.firestore.FirebaseFirestore

class Register : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextFirstName: TextInputEditText
    private lateinit var editTextLastName: TextInputEditText
    private lateinit var editTextPhoneNumber: TextInputEditText
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
        editTextPassword = findViewById(R.id.password)
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
            val password = editTextPassword.text.toString()
            val firstName = editTextFirstName.text.toString()
            val lastName = editTextLastName.text.toString()
            val phoneNumber = editTextPhoneNumber.text.toString()

            if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password) ||
                TextUtils.isEmpty(firstName) || TextUtils.isEmpty(lastName) ||
                TextUtils.isEmpty(phoneNumber)
            ) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }else if(password.length<6){
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            }
            else{
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener { task ->
                        progressBar.visibility = View.GONE
                        if (task.isSuccessful) {
                            // Registration successful
                            val user = auth.currentUser
                            val uid = user?.uid ?: ""

                            // Save additional user details to Firestore
                            val db = FirebaseFirestore.getInstance()
                            val userRef = db.collection("Users").document(uid)

                            val userDetails = hashMapOf(
                                "Email" to email,
                                "First Name" to firstName,
                                "Last Name" to lastName,
                                "Password" to password,
                                "Phone Number" to phoneNumber
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
                            // If sign-in fails, display a message to the user.
                            Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }
}