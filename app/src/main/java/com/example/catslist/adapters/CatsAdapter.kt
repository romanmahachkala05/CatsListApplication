package com.example.catslist.adapters

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.databinding.BindingAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.catslist.R
import com.example.catslist.databinding.ItemCatBinding
import com.example.catslist.models.Cat

interface CatsActionsListener {

    fun onAddToFavorites(cat: Cat)

    fun onDownload(cat: Cat)

}

class CatsAdapter(
    private val actionsListener: CatsActionsListener
) : RecyclerView.Adapter<CatsAdapter.CatsViewHolder>(), View.OnClickListener {

    private val tag = "CatsAdapter"
    var catsList: List<Cat> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onClick(v: View) {
        val cat = v.tag as? Cat
        when(v.id){
            R.id.item_cat_download_image_button -> {
                if (cat != null) {
                    actionsListener.onDownload(cat)
                } else {
                    Log.v(tag, "onDownload(), cat = null")
                }
            }
            R.id.item_cat_favorite_button -> {
                //TODO add to favorites button
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatsViewHolder {
        Log.v(tag, "onCreateViewHolder()")
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCatBinding.inflate(inflater, parent,false)

        binding.itemCatDownloadImageButton.setOnClickListener(this)

        return CatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatsViewHolder, position: Int) {
        with(holder.binding){
            cat = catsList[position]
            itemCatDownloadImageButton.tag = cat
        }
    }

    override fun getItemCount(): Int {
        return catsList.size
    }

    class CatsViewHolder(
        val binding: ItemCatBinding
    ) : RecyclerView.ViewHolder(binding.root)

}


