package com.example.catslist.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.R
import com.example.catslist.databinding.ItemCatBinding
import com.example.catslist.models.Cat
import com.example.catslist.tools.CatStorage

interface CatsActionsListener {

    fun onAddToFavorites(cat: Cat, view: View)

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
        if (cat != null) {
            when (v.id) {
                R.id.item_cat_download_image_button -> {
                    actionsListener.onDownload(cat)
                }
                R.id.item_cat_star_button -> {
                    actionsListener.onAddToFavorites(cat, v)
                }
            }
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
        with(holder.binding) {
            cat = catsList[position]
            if (!catsList[position].favorite) {
                itemCatStarButton.setBackgroundResource(R.drawable.ic_star_empty)
            } else {
                itemCatStarButton.setBackgroundResource(R.drawable.ic_star_filled)
            }
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


