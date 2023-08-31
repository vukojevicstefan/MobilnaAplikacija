package com.example.rmasprojekat.fragments

import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.rmasprojekat.R
import com.example.rmasprojekat.databinding.FragmentRegisterBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterFragment : Fragment() {

    private lateinit var editTextUsername: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var editTextFirstName: TextInputEditText
    private lateinit var editTextLastName: TextInputEditText
    private lateinit var editTextPhoneNumber: TextInputEditText
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextConfirmPassword: TextInputEditText
    private lateinit var buttonReg: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var progressBar: ProgressBar
    private lateinit var goToLogIn: TextView
    private lateinit var binding: FragmentRegisterBinding
    private var imageUri: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFragmentResultListener(CameraFragment.REQUEST_PHOTO) { _, bundle ->
            val result = bundle.getString(CameraFragment.PHOTO_URI)
            Glide.with(requireContext()).load(result).apply(
                RequestOptions.circleCropTransform()
            ).into(binding.addAPhoto)
            imageUri = result
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Register"
        auth = FirebaseAuth.getInstance()

        editTextEmail = view.findViewById(R.id.email)
        editTextUsername = view.findViewById(R.id.username)
        editTextPassword = view.findViewById(R.id.password)
        editTextConfirmPassword = view.findViewById(R.id.confirmPassword)
        editTextFirstName = view.findViewById(R.id.first_name)
        editTextLastName = view.findViewById(R.id.last_name)
        editTextPhoneNumber = view.findViewById(R.id.phone_number)

        buttonReg = view.findViewById(R.id.btn_register)
        progressBar = view.findViewById(R.id.progressBar)
        goToLogIn = view.findViewById(R.id.loginNow)

        goToLogIn.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        binding.addAPhoto.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_cameraFragment)
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
            val imageUri = imageUri
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(firstName)
                || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(phoneNumber)
            ) {
                Toast.makeText(requireContext(), "Please fill in all empty fields.", Toast.LENGTH_SHORT).show()
                progressBar.visibility = View.GONE
                return@setOnClickListener
            } else {
                isUsernameTaken(username) { uTaken ->
                    if (uTaken) {
                        Toast.makeText(requireContext(), "Username is taken.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken
                    } else if (password.length < 6) {
                        Toast.makeText(requireContext(), "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken
                    } else if(password!=secondPassword){
                        Toast.makeText(requireContext(), "Passwords must match.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken
                    }else if(imageUri == null){
                        Toast.makeText(requireContext(), "Please add a photo.", Toast.LENGTH_SHORT).show()
                        progressBar.visibility = View.GONE
                        return@isUsernameTaken
                    }
                    else {
                        binding.email.visibility = View.VISIBLE
                        binding.username.visibility = View.GONE
                        binding.password.visibility = View.GONE
                        binding.confirmPassword.visibility = View.GONE
                        binding.firstName.visibility = View.GONE
                        binding.lastName.visibility = View.GONE
                        binding.phoneNumber.visibility = View.GONE
                        binding.addAPhoto.visibility = View.GONE
                        binding.btnRegister.visibility = View.GONE

                        auth.createUserWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                progressBar.visibility = View.GONE
                                if (task.isSuccessful) {
                                    val user = auth.currentUser
                                    val uid = user?.uid ?: ""

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
                                                        // Additional user details saved successfully
                                                        Toast.makeText(requireContext(), "Registration successful.", Toast.LENGTH_SHORT).show()
                                                        findNavController().navigate(R.id.action_registerFragment_to_mapFragment)
                                                    }
                                                    .addOnFailureListener {
                                                        // Error saving additional user details
                                                        Toast.makeText(requireContext(), "Error saving user details.", Toast.LENGTH_SHORT).show()
                                                    }
                                            } else {
                                                val exception = task.exception
                                                if (exception != null) {
                                                    Log.e("Profile Update Error", exception.toString())
                                                }
                                                Toast.makeText(requireContext(), "Profile update failed.", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                } else {
                                    val exception = task.exception
                                    if (exception != null) {
                                        Log.e("Registration Error", exception.toString())
                                    }
                                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_SHORT).show()
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