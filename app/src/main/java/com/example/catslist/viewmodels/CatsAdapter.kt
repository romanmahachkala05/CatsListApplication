package com.example.catslist.viewmodels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.databinding.ItemCatBinding
import com.example.catslist.models.Cat

class CatsAdapter : RecyclerView.Adapter<CatsAdapter.CatsViewHolder>() {

    var catsList: List<Cat> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCatBinding.inflate(inflater, parent,false)
        return CatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatsViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int = catsList.size

    class CatsViewHolder(
        val binding: ItemCatBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
