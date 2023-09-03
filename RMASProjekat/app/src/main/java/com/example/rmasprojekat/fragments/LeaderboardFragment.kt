package com.example.rmasprojekat.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.firestore.FirebaseFirestore
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.User

class LeaderboardFragment : Fragment() {

    private lateinit var listView: ListView

    // This method inflates the layout for the fragment and returns the root view.
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_leaderboard, container, false)
    }

    // This method is called after the view has been created.
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set the title of the action bar to "Leaderboard".
        (requireActivity() as AppCompatActivity).supportActionBar?.title = "Leaderboard"

        // Find the ListView widget in the fragment's layout.
        listView = view.findViewById(R.id.lv_Users)

        // Initialize Firebase Firestore.
        val db = FirebaseFirestore.getInstance()
        val usersCollection = db.collection("Users")

        // Fetch users and sort them by score in descending order.
        usersCollection.orderBy("score", com.google.firebase.firestore.Query.Direction.DESCENDING)
        .get()
        .addOnSuccessListener { documents ->
            val userList = mutableListOf<User>()

            // Iterate through the Firestore documents and convert them to User objects.
            for (document in documents) {
                val user = document.toObject(User::class.java)
                userList.add(user)
            }

            // Create an ArrayAdapter to display user data in the ListView.
            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_1,
                userList.map { "${it.username}: ${it.score}" }
            )

            // Set the adapter for the ListView.
            listView.adapter = adapter
        }
        .addOnFailureListener { exception ->
            // Handle any errors that occur during data retrieval.
            Log.d("Leaderboard error", exception.message.toString())
        }
    }
}