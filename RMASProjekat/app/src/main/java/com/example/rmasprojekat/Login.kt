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

class Login : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonLogin: Button
    private lateinit var auth : FirebaseAuth
    private lateinit var progressBar : ProgressBar
    private lateinit var goToRegister: TextView

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        auth=FirebaseAuth.getInstance()
        editTextEmail = findViewById(R.id.email)
        editTextPassword = findViewById(R.id.password)
        buttonLogin = findViewById(R.id.btn_login)
        progressBar = findViewById(R.id.progressBar)
        goToRegister=findViewById(R.id.registerNow)

        goToRegister.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, Register::class.java)
            startActivity(intent)
        })

        buttonLogin.setOnClickListener(View.OnClickListener {
            progressBar.visibility=View.VISIBLE
            val email:String = editTextEmail.text.toString()
            val password:String = editTextPassword.text.toString()

            if(TextUtils.isEmpty(email)){
                Toast.makeText(this@Login,"Enter E-mail", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if(TextUtils.isEmpty(password)){
                Toast.makeText(this@Login,"Enter Password", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    progressBar.visibility=View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@Login,
                            "Login Successful.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)

                    } else {
                        Toast.makeText(
                            this@Login,
                            "Authentication failed.",
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                }
        })
    }
}