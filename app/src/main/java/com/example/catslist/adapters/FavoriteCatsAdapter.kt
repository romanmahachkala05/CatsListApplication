package com.example.catslist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.R
import com.example.catslist.databinding.ItemFavoriteCatBinding
import com.example.catslist.domain.model.Cat

interface FavoriteCatsActionsListener {

    fun onAddToFavorites(cat: Cat)
    fun onDownload(cat: Cat)

}

class FavoriteCatsAdapter(
    private val actionsListener: FavoriteCatsActionsListener
) : RecyclerView.Adapter<FavoriteCatsAdapter.FavoriteCatsViewHolder>(), View.OnClickListener {

    var favoriteCatsList: List<Cat> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onClick(v: View) {
        val cat = v.tag as? Cat ?: return
        when (v.id) {
            R.id.item_cat_favorite_download_image_button -> actionsListener.onDownload(cat)
            R.id.item_cat_favorite_star_button -> actionsListener.onAddToFavorites(cat)
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
        val cat = favoriteCatsList[position]
        with(holder.binding) {
            this.cat = cat
            itemCatFavoriteStarButton.setBackgroundResource(
                if (cat.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
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
