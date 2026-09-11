package com.example.catslist.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.R
import com.example.catslist.databinding.ItemCatBinding
import com.example.catslist.domain.model.Cat

interface CatsActionsListener {

    fun onAddToFavorites(cat: Cat)

    fun onDownload(cat: Cat)

}

class CatsAdapter(
    private val actionsListener: CatsActionsListener
) : RecyclerView.Adapter<CatsAdapter.CatsViewHolder>(), View.OnClickListener {

    var catsList: List<Cat> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onClick(v: View) {
        val cat = v.tag as? Cat ?: return
        when (v.id) {
            R.id.item_cat_download_image_button -> actionsListener.onDownload(cat)
            R.id.item_cat_star_button -> actionsListener.onAddToFavorites(cat)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCatBinding.inflate(inflater, parent, false)

        binding.itemCatDownloadImageButton.setOnClickListener(this)
        binding.itemCatStarButton.setOnClickListener(this)

        return CatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatsViewHolder, position: Int) {
        val cat = catsList[position]
        with(holder.binding) {
            this.cat = cat
            itemCatStarButton.setBackgroundResource(
                if (cat.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
            )
            itemCatDownloadImageButton.tag = cat
            itemCatStarButton.tag = cat
        }
    }

    override fun getItemCount(): Int {
        return catsList.size
    }

    class CatsViewHolder(
        val binding: ItemCatBinding
    ) : RecyclerView.ViewHolder(binding.root)

}
