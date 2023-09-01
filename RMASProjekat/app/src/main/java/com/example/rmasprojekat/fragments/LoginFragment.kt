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

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: Button
    private lateinit var auth : FirebaseAuth
    private lateinit var progressBar : ProgressBar
    private lateinit var goToRegister: TextView
    private lateinit var viewModel: LoginViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val activity = requireActivity() as MainActivity
            viewModel.getCurrentUserFromFirestore(currentUser.uid){
                if (it != null) {
                activity.userViewModel.setCurrentUser(it)
                Toast.makeText(requireContext(), "Welcome ${it.username}!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.mapFragment)
                } else {
                    Toast.makeText(requireContext(), "Error 111 occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Login"


        progressBar = view.findViewById(R.id.progressBar)
        auth = FirebaseAuth.getInstance()
        editTextEmail = view.findViewById(R.id.email)
        editTextPassword = view.findViewById(R.id.password)
        buttonLogin = view.findViewById(R.id.btn_login)
        goToRegister = view.findViewById(R.id.registerNow)

        val activity = requireActivity() as MainActivity
        viewModel = LoginViewModel(activity.userViewModel)

        goToRegister.setOnClickListener {
            findNavController().navigate(R.id.registerFragment)
        }
        buttonLogin.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            val email: String = editTextEmail.text.toString()
            val password: String = editTextPassword.text.toString()

            if (TextUtils.isEmpty(email)) {
                // Handle email validation
                return@setOnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                // Handle password validation
                return@setOnClickListener
            }

            viewModel.loginUserWithEmailAndPassword(email, password) { success ->
                progressBar.visibility = View.GONE
                if (success) { Toast.makeText(requireContext(), "Login Successful.", Toast.LENGTH_SHORT,).show()
                    findNavController().navigate(R.id.mapFragment)
                } else {
                    Toast.makeText(requireContext(), "Authentication failed.", Toast.LENGTH_LONG,).show()
                }
            }
        }
    }
}
