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
import androidx.fragment.app.setFragmentResultListener
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.rmasprojekat.MainActivity
import com.example.rmasprojekat.R
import com.example.rmasprojekat.databinding.FragmentRegisterBinding
import com.example.rmasprojekat.viewmodels.RegisterViewModel
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

class RegisterFragment : Fragment() {

    // Declare UI components and variables
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
    private lateinit var viewModel: RegisterViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set up a listener for receiving a result from the CameraFragment
        setFragmentResultListener(CameraFragment.REQUEST_PHOTO) { _, bundle ->
            // Retrieve the image URI from the result bundle
            val result = bundle.getString(CameraFragment.PHOTO_URI)

            // Load and display the image using Glide library
            Glide.with(requireContext()).load(result).apply(
                RequestOptions.circleCropTransform()
            ).into(binding.addAPhoto)

            // Store the image URI for later use
            imageUri = result
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment using ViewBinding
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get a reference to the parent activity and create a ViewModel
        val activity = requireActivity() as MainActivity
        viewModel = RegisterViewModel(activity.userViewModel)

        // Set the title of the action bar
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Register"

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()

        // Initialize UI components by finding their views
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

        // Navigate to the login fragment when the "Login Now" text is clicked
        goToLogIn.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_loginFragment)
        }

        // Navigate to the CameraFragment when the "Add a Photo" button is clicked
        binding.addAPhoto.setOnClickListener {
            findNavController().navigate(R.id.action_registerFragment_to_cameraFragment)
        }

        // Register button click listener
        buttonReg.setOnClickListener {
            setEditTextEnabled(false)
            progressBar.visibility = View.VISIBLE

            // Retrieve user input data from the EditText fields
            val email = editTextEmail.text.toString()
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()
            val secondPassword = editTextConfirmPassword.text.toString()
            val firstName = editTextFirstName.text.toString()
            val lastName = editTextLastName.text.toString()
            val phoneNumber = editTextPhoneNumber.text.toString()
            val imageUri = imageUri

            // Check if any required field is empty
            if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email)
                || TextUtils.isEmpty(password) || TextUtils.isEmpty(firstName)
                || TextUtils.isEmpty(lastName) || TextUtils.isEmpty(phoneNumber)
            ) {
                Toast.makeText(requireContext(), "Please fill in all empty fields.", Toast.LENGTH_SHORT).show()
                setEditTextEnabled(true)
                progressBar.visibility = View.GONE
                return@setOnClickListener
            } else {
                // Check if the username is already taken
                viewModel.isUsernameTaken(username) { uTaken ->
                    if (uTaken) {
                        setEditTextEnabled(true)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Username is taken.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    } else if (password.length < 6) {
                        setEditTextEnabled(true)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    } else if (password != secondPassword) {
                        setEditTextEnabled(true)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Passwords must match.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    } else if (imageUri == null) {
                        setEditTextEnabled(true)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Please add a photo.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    } else {
                        // Create a user account with Firebase Authentication
                        viewModel.createUserWithEmailAndPassword(email, password, username, firstName, lastName, phoneNumber, imageUri) { success ->
                            if (success) {
                                // Navigate to the map fragment on successful registration
                                findNavController().navigate(R.id.mapFragment)
                            } else {
                                progressBar.visibility = View.GONE
                                setEditTextEnabled(true)
                                Toast.makeText(requireContext(), "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
    // Helper function to enable/disable UI components
    private fun setEditTextEnabled(enabled: Boolean) {
        binding.email.isEnabled = enabled
        binding.username.isEnabled = enabled
        binding.password.isEnabled = enabled
        binding.confirmPassword.isEnabled = enabled
        binding.firstName.isEnabled = enabled
        binding.lastName.isEnabled = enabled
        binding.phoneNumber.isEnabled = enabled
        binding.addAPhoto.isEnabled = enabled
        binding.btnRegister.isEnabled = enabled
    }
}
