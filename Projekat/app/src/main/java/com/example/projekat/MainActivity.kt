package com.example.projekat

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.projekat.ui.theme.ProjekatTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class MainActivity : ComponentActivity() {

        private lateinit var auth: FirebaseAuth
        private lateinit var buttonLogOut: Button
        private lateinit var textView: TextView
        private lateinit var user:FirebaseUser
        override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)

                auth=FirebaseAuth.getInstance()
                buttonLogOut=findViewById<Button>(R.id.logout)
                textView=findViewById<TextView>(R.id.user_details)
                user= auth.currentUser!!
                var intent:Intent
                if(user==null){
                        intent=Intent(applicationContext,Login::class.java)
                        startActivity(intent)
                        finish()
                }
                else{
                        textView.text=user.email

                }
                buttonLogOut.setOnClickListener(View.OnClickListener {
                        FirebaseAuth.getInstance().signOut()

                        intent=Intent(this,Login::class.java)
                        startActivity(intent)
                })
        }
}