package com.example.catslist.views

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.example.catslist.App
import com.example.catslist.R
import com.example.catslist.adapters.FavoriteCatsActionsListener
import com.example.catslist.adapters.FavoriteCatsAdapter
import com.example.catslist.databinding.FragmentFavoriteCatsListBinding
import com.example.catslist.models.Cat
import com.example.catslist.tools.CatStorage
import com.example.catslist.tools.CatsListener
import com.example.catslist.viewmodels.FavoriteCatsListFragmentViewModel

class FavoriteCatsListFragment : Fragment() {

    private lateinit var adapter: FavoriteCatsAdapter
    private lateinit var binding: FragmentFavoriteCatsListBinding
    private val catsStorage: CatStorage
        get() = (requireActivity().applicationContext as App).catsService

    companion object {
        fun newInstance() = FavoriteCatsListFragment()
    }

    private lateinit var viewModel: FavoriteCatsListFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(activity),
            R.layout.fragment_favorite_cats_list,
            container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this)[FavoriteCatsListFragmentViewModel::class.java]
        initialize()
    }

    private fun initialize(){
        initRecyclerView()
        CatStorage.addFavListener(favoriteCatsListener)
    }

    private fun initRecyclerView(){
        adapter = FavoriteCatsAdapter(object : FavoriteCatsActionsListener {
            override fun onAddToFavorites(cat: Cat, view: View) {
                viewModel.onFavoriteButtonClick(cat)
            }

            override fun onDownload(cat: Cat) {
                viewModel.downloadCatImage(requireContext(), cat.url, cat.id)
            }

        })
        catsStorage.addFavListener(favoriteCatsListener)
        binding.recyclerViewWithFavoriteCats.adapter = adapter
    }

    private val favoriteCatsListener: CatsListener = {
        adapter.favoriteCatsList = it
    }
}