package com.example.catslist.viewmodels

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.databinding.ItemCatBinding
import com.example.catslist.models.Cat

class CatsAdapter : RecyclerView.Adapter<CatsAdapter.CatsViewHolder>() {

    private val tag = "CatsAdapter"
    var catsList: List<Cat> = emptyList()
        set(newValue) {
            field = newValue
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatsViewHolder {
        Log.v(tag, "onCreateViewHolder()")
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemCatBinding.inflate(inflater, parent,false)
        return CatsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CatsViewHolder, position: Int) {
        Log.v(tag, "onBindViewHolder()")
        with(holder.binding){
            cat = catsList[position]
        }
    }

    override fun getItemCount(): Int {
        Log.v(tag, "getItemCount(), value = ${catsList.size}")
        return catsList.size
    }

    class CatsViewHolder(
        val binding: ItemCatBinding
    ) : RecyclerView.ViewHolder(binding.root)
}
