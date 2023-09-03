// Import necessary Android libraries and classes
package com.example.rmasprojekat.fragments

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.rmasprojekat.MainActivity
import com.example.rmasprojekat.R
import com.example.rmasprojekat.databinding.FragmentLoginBinding
import com.example.rmasprojekat.viewmodels.LoginViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

// Define the LoginFragment class, which extends Fragment
class LoginFragment : Fragment() {

    // Declare class-level variables
    private lateinit var binding: FragmentLoginBinding // Binding object for the fragment layout
    private lateinit var editTextEmail: TextInputEditText // Input field for email
    private lateinit var editTextPassword: TextInputEditText // Input field for password
    private lateinit var buttonLogin: Button // Button to trigger login
    private lateinit var auth: FirebaseAuth // Firebase authentication instance
    private lateinit var progressBar: ProgressBar // Progress bar to show loading
    private lateinit var goToRegister: TextView // TextView to navigate to registration
    private lateinit var viewModel: LoginViewModel // ViewModel for login functionality

    // Override the onCreateView method to inflate the fragment's layout
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    // Override the onStart method to check if a user is already logged in
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // If a user is logged in, fetch user data and navigate to the main map screen
            val activity = requireActivity() as MainActivity
            viewModel.getCurrentUserFromFirestore(currentUser.uid) { user ->
                if (user != null) {
                    activity.userViewModel.setCurrentUser(user)
                    Toast.makeText(requireContext(), "Welcome ${user.username}!", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.mapFragment)
                } else {
                    Toast.makeText(requireContext(), "Error occurred while fetching user data", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Override the onViewCreated method to set up UI elements and click listeners
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Login"

        // Initialize UI elements
        progressBar = view.findViewById(R.id.progressBar)
        auth = FirebaseAuth.getInstance()
        editTextEmail = view.findViewById(R.id.email)
        editTextPassword = view.findViewById(R.id.password)
        buttonLogin = view.findViewById(R.id.btn_login)
        goToRegister = view.findViewById(R.id.registerNow)

        // Get a reference to the MainActivity and initialize the ViewModel
        val activity = requireActivity() as MainActivity
        viewModel = LoginViewModel(activity.userViewModel)

        // Set a click listener on the "Register Now" TextView to navigate to the registration screen
        goToRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }

        // Set a click listener on the login button
        buttonLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val email: String = editTextEmail.text.toString()
            val password: String = editTextPassword.text.toString()

            if (TextUtils.isEmpty(email)) {
                // Handle email validation (not implemented in this code)
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                // Handle password validation (not implemented in this code)
                return@setOnClickListener
            }

            // Call the ViewModel's loginUserWithEmailAndPassword function to authenticate the user
            viewModel.loginUserWithEmailAndPassword(email, password) { success ->
                progressBar.visibility = View.GONE
                if (success) {
                    // If login is successful, display a success message and navigate to the main map screen
                    Toast.makeText(requireContext(), "Login Successful.", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.mapFragment)
                } else {
                    // If login fails, display an error message
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
