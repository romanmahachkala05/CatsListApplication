package com.example.catslist.views

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.catslist.R
import com.example.catslist.adapters.CatsActionsListener
import com.example.catslist.adapters.CatsAdapter
import com.example.catslist.databinding.FragmentCatsListBinding
import com.example.catslist.models.Cat
import com.example.catslist.tools.CatStorage
import com.example.catslist.tools.CatsListener
import com.example.catslist.viewmodels.CatsListFragmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CatsListFragment : Fragment() {

    private lateinit var adapter: CatsAdapter
    private lateinit var binding: FragmentCatsListBinding

    companion object {
        fun newInstance() = CatsListFragment()
    }

    private val viewModel: CatsListFragmentViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(
            LayoutInflater.from(activity),
            R.layout.fragment_cats_list,
            container, false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = CatsAdapter(object : CatsActionsListener {
            override fun onAddToFavorites(cat: Cat, view: View) {
                viewModel.onFavoriteButtonClick(cat, view)
            }

            override fun onDownload(cat: Cat) {
                viewModel.downloadCatImage(requireContext(), cat.url, cat.id)
            }

        })

        binding.recyclerViewWithCats.adapter = adapter
        binding.recyclerViewWithCats.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    addCat()
                }
            }
        })

        CatStorage.addListener(catsListener)
        addCat()
    }

    fun addCat() {
        repeat(5) { viewModel.addCat() }
    }

    private val catsListener: CatsListener = {
        adapter.catsList = it
    }
}