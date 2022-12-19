package com.example.catslist.views

import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.catslist.R
import com.example.catslist.viewmodels.CatsListFragmentViewModel

class CatsListFragment : Fragment() {

    companion object {
        fun newInstance() = CatsListFragment()
    }

    private lateinit var viewModel: CatsListFragmentViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_cats_list, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProvider(this).get(CatsListFragmentViewModel::class.java)
        // TODO: Use the ViewModel
    }

}