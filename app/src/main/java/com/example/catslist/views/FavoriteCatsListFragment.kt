package com.example.catslist.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.catslist.R
import com.example.catslist.adapters.FavoriteCatsActionsListener
import com.example.catslist.adapters.FavoriteCatsAdapter
import com.example.catslist.databinding.FragmentFavoriteCatsListBinding
import com.example.catslist.domain.model.Cat
import com.example.catslist.viewmodels.FavoriteCatsListFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class FavoriteCatsListFragment : Fragment() {

    private lateinit var adapter: FavoriteCatsAdapter
    private lateinit var binding: FragmentFavoriteCatsListBinding

    companion object {
        fun newInstance() = FavoriteCatsListFragment()
    }

    private val viewModel: FavoriteCatsListFragmentViewModel by viewModels()

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

        adapter = FavoriteCatsAdapter(object : FavoriteCatsActionsListener {
            override fun onAddToFavorites(cat: Cat) {
                viewModel.onFavoriteButtonClick(cat)
            }

            override fun onDownload(cat: Cat) {
                viewModel.downloadCatImage(requireContext(), cat.url, cat.id)
            }

        })
        binding.recyclerViewWithFavoriteCats.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteCats.collect { adapter.favoriteCatsList = it }
            }
        }
    }
}
