package com.example.catslist.adapters

import android.content.Context
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide
import com.example.catslist.models.Cat

@BindingAdapter("catImage")
fun setCatImage(view: ImageView, cat: Cat) {
    view.setImageFromUrl(view.context, cat.url)
}

private fun ImageView.setImageFromUrl(context: Context, url: String) {
    Glide.with(context)
        .asBitmap()
        .load(url)
        .centerCrop()
        .thumbnail()
        .into(this)
}