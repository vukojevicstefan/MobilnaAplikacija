package com.example.rmasprojekat.adapters

import android.annotation.SuppressLint
import android.text.TextUtils.isEmpty
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.rmasprojekat.R
import com.example.rmasprojekat.data.Review
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ReviewsAdapter(private val reviews: MutableList<Review>) :
    RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvUserName: TextView = itemView.findViewById(R.id.tvUsername)
        val ratingBarReview: RatingBar = itemView.findViewById(R.id.ratingBarReview)
        val tvComment: TextView = itemView.findViewById(R.id.tvComment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(itemView)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        val currentReview = reviews[position]
        val currentUser = FirebaseAuth.getInstance().currentUser
        holder.tvUserName.text = "By " + currentReview.user
        holder.ratingBarReview.rating = currentReview.rating.toFloat()
        holder.tvComment.text = currentReview.text
        if(isEmpty(currentReview.text))
            holder.tvComment.visibility=View.GONE


        val btnLike: ImageButton = holder.itemView.findViewById(R.id.btnLike)
        val btnUnlike: ImageButton = holder.itemView.findViewById(R.id.btnUnlike)
        val tvLikes: TextView = holder.itemView.findViewById(R.id.likes)

        tvLikes.text = if (review.likes == 1) "${review.likes} like" else "${review.likes} likes"

        if (currentUser != null) {
            val userId = currentUser.uid
            fetchLikedReviewsForUser(userId) { likedReviews ->
                if (likedReviews.contains(review.id)) {
                    // The review is liked by the current user
                    btnLike.visibility = View.INVISIBLE
                    btnUnlike.visibility = View.VISIBLE
                } else {
                    // The review is not liked by the current user
                    btnLike.visibility = View.VISIBLE
                    btnUnlike.visibility = View.INVISIBLE
                }
            }

            btnLike.setOnClickListener {
                if (currentUser != null) {
                    review.likes++
                    updateReviewLikes(review.id, review.likes, review.markerId)
                    getUserIdFromEmail(currentUser.email.toString()) { userId ->
                        if (userId != null) {
                            Log.d("MyApp",userId)
                            updateUserLikedReviews(userId, review.id)
                            getUserIdFromUsername(currentReview.user) { authorId ->
                                if (authorId != null) {
                                    addToAuthorScore(authorId)
                                }
                            }
                        } else {
                            Log.d("MyApp", "User not found with the provided email.")
                        }
                    }

                    tvLikes.text =
                        if (review.likes == 1) "${review.likes} like" else "${review.likes} likes"
                    btnLike.visibility = View.INVISIBLE
                    btnUnlike.visibility = View.VISIBLE
                }
            }

            btnUnlike.setOnClickListener {
                if (currentUser != null) {
                    review.likes--
                    updateReviewLikes(review.id, review.likes, review.markerId)
                    getUserIdFromEmail(currentUser.email.toString()) { userId ->
                        if (userId != null) {
                            removeUserLikedReview(userId, review.id)
                            getUserIdFromUsername(currentReview.user) { authorId ->
                                if (authorId != null) {
                                    subtractFromAuthorScore(authorId)
                                }
                            }
                        } else {
                            Log.d("MyApp", "User not found with the provided email.")
                        }
                    }

                    tvLikes.text =
                        if (review.likes == 1) "${review.likes} like" else "${review.likes} likes"
                    btnLike.visibility = View.VISIBLE
                    btnUnlike.visibility = View.INVISIBLE
                }
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
    fun updateReviews(updatedReviews: List<Review>) {
        reviews.clear()
        reviews.addAll(updatedReviews)
        notifyDataSetChanged()
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
    private fun getUserIdFromEmail(userEmail: String, onComplete: (String?) -> Unit) {
        val db = Firebase.firestore
        val usersCollection = db.collection("Users")

        usersCollection.whereEqualTo("email", userEmail)
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
    private fun fetchLikedReviewsForUser(userId: String, callback: (List<String>) -> Unit) {
        val db = Firebase.firestore
        val userRef = db.collection("Users").document(userId)

        userRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val likedReviews = mutableListOf<String>()
                if (documentSnapshot.exists()) {
                    val likedReviewsData = documentSnapshot.get("likedReviews") as? List<String>
                    if (likedReviewsData != null) {
                        likedReviews.addAll(likedReviewsData)
                    }
                }
                callback(likedReviews)
            }
            .addOnFailureListener { e ->
                Log.e("MyApp", "Error fetching liked reviews: $e")
                callback(emptyList())
            }
    }
//    private fun reviewBelongsToUser(username: String, callback: (Boolean) -> Unit):Boolean {
//        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
//
//        if (currentUserId != null) {
//            val db = FirebaseFirestore.getInstance()
//            val usersCollection = db.collection("Users")
//
//            usersCollection.document(currentUserId)
//                .get()
//                .addOnSuccessListener { documentSnapshot ->
//                    if (documentSnapshot.exists()) {
//                        val currentUserUsername = documentSnapshot.getString("username")
//                        callback(currentUserUsername == username)
//                    } else {
//                        callback(false)
//                    }
//                }
//                .addOnFailureListener { exception ->
//                    Log.e("Error","Error finding user.")
//                    callback(false)
//                }
//        }
//        return false
//    }
//    private fun deleteReview(markerId: String, reviewId: String) {
//        val db = FirebaseFirestore.getInstance()
//        val markersCollection = db.collection("Markers")
//
//        val markerDocument = markersCollection.document(markerId)
//
//        val reviewsCollection = markerDocument.collection("reviews")
//
//        val reviewDocument = reviewsCollection.document(reviewId)
//
//        reviewDocument.delete()
//            .addOnSuccessListener{
//                deleteListener?.onReviewDeleted()
//                Log.d("Delete Review", "Review successfully deleted")
//            }
//            .addOnFailureListener { e ->
//                Log.e("Delete Review", "Error deleting review: $e")
//            }
//    }
//    interface ReviewDeleteListener {
//        fun onReviewDeleted()
//    }
//    fun setReviewDeleteListener(listener: ReviewDeleteListener) {
//        deleteListener = listener
//    }
}