package com.example.catslist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.R
import com.example.catslist.databinding.ItemFavoriteCatBinding
import com.example.catslist.models.Cat

interface FavoriteCatsActionsListener {

    fun onAddToFavorites(cat: Cat)

    fun onDownload(cat: Cat)

}

class FavoriteCatsAdapter(
    private val actionsListener: CatsActionsListener
) : RecyclerView.Adapter<FavoriteCatsAdapter.FavoriteCatsViewHolder>(), View.OnClickListener {

    private val tag = "FavoriteCatsAdapter"
    var favoriteCatsList: List<Cat> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onClick(v: View) {
        val cat = v.tag as? Cat
        when(v.id){
            R.id.item_favorite_cat_download_image_button -> {
                if (cat != null) {
                    actionsListener.onDownload(cat)
                } else {
                    Log.v(tag, "onDownload(), cat = null")
                }
            }
            else -> {
                //TODO add to favorites button
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteCatsViewHolder {
        Log.v(tag, "onCreateViewHolder()")
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFavoriteCatBinding.inflate(inflater, parent,false)

        binding.itemFavoriteCatDownloadImageButton.setOnClickListener(this)

        return FavoriteCatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteCatsViewHolder, position: Int) {
        with(holder.binding){
            cat = favoriteCatsList[position]
            itemFavoriteCatDownloadImageButton.tag = cat
        }
    }

    override fun getItemCount(): Int {
        return favoriteCatsList.size
    }


    class FavoriteCatsViewHolder(
        val binding: ItemFavoriteCatBinding
    ) : RecyclerView.ViewHolder(binding.root)

}