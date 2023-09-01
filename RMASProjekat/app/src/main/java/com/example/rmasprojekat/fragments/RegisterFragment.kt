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

        val activity = requireActivity() as MainActivity
        viewModel = RegisterViewModel(activity.userViewModel)
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
            setVisibility(View.GONE)
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
                viewModel.isUsernameTaken(username) { uTaken ->
                    if (uTaken) {
                        setVisibility(View.VISIBLE)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Username is taken.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    } else if (password.length < 6) {
                        setVisibility(View.VISIBLE)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Password must be at least 6 characters long.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    } else if(password!=secondPassword){
                        setVisibility(View.VISIBLE)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Passwords must match.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    }else if(imageUri == null){
                        setVisibility(View.VISIBLE)
                        progressBar.visibility = View.GONE
                        Toast.makeText(requireContext(), "Please add a photo.", Toast.LENGTH_SHORT).show()
                        return@isUsernameTaken
                    }
                    else {
                        viewModel.createUserWithEmailAndPassword(email,password,username,firstName,lastName,phoneNumber,imageUri){ success ->
                            if (success) {
                                findNavController().navigate(R.id.mapFragment)
                            } else {
                                progressBar.visibility = View.GONE
                                setVisibility(View.VISIBLE)
                                Toast.makeText(requireContext(), "Registration failed. Please try again.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }
    private fun setVisibility(visibility:Int){
        binding.email.visibility = visibility
        binding.username.visibility = visibility
        binding.password.visibility = visibility
        binding.confirmPassword.visibility = visibility
        binding.firstName.visibility = visibility
        binding.lastName.visibility = visibility
        binding.phoneNumber.visibility = visibility
        binding.addAPhoto.visibility = visibility
        binding.btnRegister.visibility = visibility
    }
}