package com.example.rmasprojekat.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.rmasprojekat.R

// Create an adapter class named ImagesAdapter that extends RecyclerView.Adapter
class ImagesAdapter(private val imageUrls: List<String>) :
    RecyclerView.Adapter<ImagesAdapter.ImageViewHolder>() {

    // Create a ViewHolder class for individual list items
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageView)
    }

    // Override onCreateViewHolder method to inflate the item layout
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        // Inflate the item layout using a LayoutInflater
        val itemView =
            LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        return ImageViewHolder(itemView)
    }

    // Override onBindViewHolder to bind data to the ViewHolder
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        // Use the Glide library to load an image from the URL and display it in the ImageView
        Glide.with(holder.itemView)
            .load(imageUrls[position])
            .into(holder.imageView)
    }

    // Override getItemCount to specify the number of items in the RecyclerView
    override fun getItemCount(): Int = imageUrls.size
}