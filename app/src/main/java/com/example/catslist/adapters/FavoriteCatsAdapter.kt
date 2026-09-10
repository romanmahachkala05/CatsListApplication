package com.example.catslist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.R
import com.example.catslist.databinding.ItemFavoriteCatBinding
import com.example.catslist.models.Cat
import com.example.catslist.tools.CatStorage

interface FavoriteCatsActionsListener {

    fun onAddToFavorites(cat: Cat, view: View)
    fun onDownload(cat: Cat)

}

class FavoriteCatsAdapter(
    private val actionsListener: FavoriteCatsActionsListener
) : RecyclerView.Adapter<FavoriteCatsAdapter.FavoriteCatsViewHolder>(), View.OnClickListener {

    private val tag = "FavoriteCatsAdapter"
    var favoriteCatsList: List<Cat> = CatStorage.favoriteCats
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onClick(v: View) {
        val cat = v.tag as? Cat
        if (cat != null) {
            when (v.id) {
                R.id.item_cat_favorite_download_image_button -> {
                    actionsListener.onDownload(cat)
                }
                R.id.item_cat_favorite_star_button -> {
                    actionsListener.onAddToFavorites(cat, v)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoriteCatsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemFavoriteCatBinding.inflate(inflater, parent, false)

        binding.itemCatFavoriteDownloadImageButton.setOnClickListener(this)
        binding.itemCatFavoriteStarButton.setOnClickListener(this)

        return FavoriteCatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FavoriteCatsViewHolder, position: Int) {
        with(holder.binding) {
            cat = favoriteCatsList[position]
            if (favoriteCatsList[position].favorite) itemCatFavoriteStarButton.setBackgroundResource(
                R.drawable.ic_star_filled
            )
            itemCatFavoriteDownloadImageButton.tag = cat
            itemCatFavoriteStarButton.tag = cat
        }
    }

    override fun getItemCount(): Int {
        return favoriteCatsList.size
    }

    class FavoriteCatsViewHolder(
        val binding: ItemFavoriteCatBinding
    ) : RecyclerView.ViewHolder(binding.root)
}