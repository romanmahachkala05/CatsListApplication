package com.example.catslist.viewmodels

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.databinding.ItemCatBinding

class CatsAdapter : RecyclerView.Adapter<CatsAdapter.CatsViewBinding> {

    class CatsViewBinding(
        val binding: ItemCatBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CatsViewBinding {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: CatsViewBinding, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
    }
}