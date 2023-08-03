package com.example.projekat

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth

public class Register : AppCompatActivity() {
    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonReg: Button
    private lateinit var auth :FirebaseAuth
    private lateinit var progressBar :ProgressBar
    private lateinit var goToLogIn: TextView
    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
        auth=FirebaseAuth.getInstance()
        editTextEmail = findViewById<TextInputEditText>(R.id.email)
        editTextPassword = findViewById<TextInputEditText>(R.id.password)
        buttonReg = findViewById<Button>(R.id.btn_register)
        progressBar = findViewById<ProgressBar>(R.id.progressBar)
        goToLogIn=findViewById<TextView>(R.id.loginNow)

        goToLogIn.setOnClickListener(View.OnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
        })
        buttonReg.setOnClickListener(View.OnClickListener {
            progressBar.visibility=View.VISIBLE
            var email:String
            var password:String
            email = editTextEmail.text.toString()
            password = editTextPassword.text.toString()

            if(TextUtils.isEmpty(email)){
                Toast.makeText(this@Register,"Enter E-mail",Toast.LENGTH_SHORT).show()
                return@OnClickListener;
            }
            if(TextUtils.isEmpty(password)){
                Toast.makeText(this@Register,"Enter Password",Toast.LENGTH_SHORT).show()
                return@OnClickListener;
            }
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    progressBar.visibility=View.GONE
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@Register,
                            "Registration successful.",
                            Toast.LENGTH_SHORT,
                        ).show()
                        val intent = Intent(this, Login::class.java)
                        startActivity(intent)
                    } else {
                        // If sign in fails, display a message to the user.
                        Toast.makeText(
                            this@Register,
                            "Authentication failed.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
        })
    }
}