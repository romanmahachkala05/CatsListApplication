package com.example.catslist.views

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.catslist.R
import com.example.catslist.viewmodels.FavoriteCatsListFragmentViewModel

class FavoriteCatsListFragment : Fragment() {

    companion object {
        fun newInstance() = FavoriteCatsListFragment()
    }

    private lateinit var viewModel: FavoriteCatsListFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_favorite_cats_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(FavoriteCatsListFragmentViewModel::class.java)
        // TODO: Use the ViewModel
    }

}