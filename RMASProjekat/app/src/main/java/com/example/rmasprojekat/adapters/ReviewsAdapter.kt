package com.example.rmasprojekat.adapters

import android.annotation.SuppressLint
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.Review
import com.example.rmasprojekat.viewmodels.CurrentUserViewModel
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReviewsAdapter(private val reviews: MutableList<Review>, private val currentUserViewModel: CurrentUserViewModel) :
    RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        // ViewHolder for individual review items, holding references to UI elements
        val tvUserName: TextView = itemView.findViewById(R.id.tvUsername)
        val ratingBarReview: RatingBar = itemView.findViewById(R.id.ratingBarReview)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        // Inflating the layout for an individual review item
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        // Bind data to the UI elements of a review item
        val review = reviews[position]
        val currentReview = reviews[position]
        holder.tvUserName.text = "By " + currentReview.user
        holder.ratingBarReview.rating = currentReview.rating.toFloat()
        holder.tvComment.text = currentReview.text

        // Handle visibility of comment TextView
        if(isEmpty(currentReview.text))
            holder.tvComment.visibility = View.GONE

        // Get references to like, unlike, and likes TextView elements
        val btnLike: ImageButton = holder.itemView.findViewById(R.id.btnLike)
        val btnUnlike: ImageButton = holder.itemView.findViewById(R.id.btnUnlike)
        val tvLikes: TextView = holder.itemView.findViewById(R.id.likes)

        // Display the number of likes for the review
        tvLikes.text = if (review.likes == 1) "${review.likes} like" else "${review.likes} likes"

        // Check if the current user has liked this review
        val likedReviews = currentUserViewModel.currentUser.value!!.likedReviews
        if (likedReviews.contains(review.id)) {
            btnLike.visibility = View.INVISIBLE
            btnUnlike.visibility = View.VISIBLE
        } else {
            btnLike.visibility = View.VISIBLE
            btnUnlike.visibility = View.INVISIBLE
        }

        // Handle click listeners for like and unlike buttons
        btnLike.setOnClickListener {
            // Increment review likes, update UI, and interact with Firestore
            getUserIdFromUsername(currentReview.user) { authorId ->
                review.likes++
                updateReviewLikes(review.id, review.likes, review.markerId)
                val userId = currentUserViewModel.currentUser.value!!.id
                updateUserLikedReviews(userId, review.id)
                if (authorId != null) {
                    addToAuthorScore(authorId)
                }
                currentUserViewModel.currentUser.value!!.likedReviews.add(review.id)

                tvLikes.text =
                    if (review.likes == 1) "${review.likes} like" else "${review.likes} likes"
                btnLike.visibility = View.INVISIBLE
                btnUnlike.visibility = View.VISIBLE
            }
        }

        btnUnlike.setOnClickListener {
            // Decrement review likes, update UI, and interact with Firestore
            getUserIdFromUsername(currentReview.user) { authorId ->
                if (authorId != null) {
                    subtractFromAuthorScore(authorId)
                }
                review.likes--
                updateReviewLikes(review.id, review.likes, review.markerId)
                val userId = currentUserViewModel.currentUser.value!!.id
                removeUserLikedReview(userId, review.id)
                currentUserViewModel.currentUser.value!!.likedReviews.remove(review.id)
                tvLikes.text =
                    if (review.likes == 1) "${review.likes} like" else "${review.likes} likes"
                btnLike.visibility = View.VISIBLE
                btnUnlike.visibility = View.INVISIBLE
            }
        }
    }
    private fun updateUserLikedReviews(userId: String, reviewId: String) {
        val db = Firebase.firestore
        val userRef = db.collection("Users").document(userId)
        userRef.update("likedReviews", FieldValue.arrayUnion(reviewId))
    }
    private fun removeUserLikedReview(userId: String, reviewId: String) {
        val db = Firebase.firestore
        val userRef = db.collection("Users").document(userId)
        userRef.update("likedReviews", FieldValue.arrayRemove(reviewId))
    }
    private fun addToAuthorScore(userId: String) {
        val db = Firebase.firestore
        val userRef = db.collection("Users").document(userId)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    var score = documentSnapshot.getLong("score") ?: 0
                    userRef.update("score", ++score)
                }
            }
    }
    private fun subtractFromAuthorScore(userId:String){
        val db = Firebase.firestore
        val userRef = db.collection("Users").document(userId)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    var score = documentSnapshot.getLong("score") ?: 0
                    userRef.update("score", --score)
                }
            }
    }
    override fun getItemCount(): Int {
        return reviews.size
    }
    private fun updateReviewLikes(reviewId: String, newLikesCount: Int, clickedLocationId:String) {
        val db = Firebase.firestore
        val reviewRef = db.collection("Markers").document(clickedLocationId)
            .collection("reviews").document(reviewId)

        val data = hashMapOf("likes" to newLikesCount)
        reviewRef.set(data, SetOptions.merge())
            .addOnSuccessListener {
                Log.d("MyApp", "Review likes updated successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error updating review likes: $e")
            }
    }
    private fun getUserIdFromUsername(username: String, onComplete: (String?) -> Unit) {
        val db = Firebase.firestore
        val usersCollection = db.collection("Users")

        usersCollection.whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val userDocument = documents.documents[0]
                    val userId = userDocument.id
                    onComplete(userId)
                } else {
                    onComplete(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error fetching user with email: $e")
                onComplete(null)
            }
    }
}