package com.example.rmasprojekat

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : AppCompatActivity() {
    lateinit var auth: FirebaseAuth
    lateinit var button:Button
    lateinit var textView: TextView
    lateinit var user:FirebaseUser



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth=FirebaseAuth.getInstance()
        button=findViewById(R.id.logout)
        textView=findViewById(R.id.user_details)
        user=auth.currentUser!!
        if(user==null){
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }else{
            textView.setText(user.email)
        }
        button.setOnClickListener(View.OnClickListener {
            FirebaseAuth.getInstance().signOut()

            intent=Intent(this,Login::class.java)
            startActivity(intent)
        })
    }
}