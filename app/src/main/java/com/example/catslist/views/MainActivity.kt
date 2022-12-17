package com.example.catslist.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.catslist.App
import com.example.catslist.databinding.ActivityMainBinding
import com.example.catslist.tools.CatStorage
import com.example.catslist.tools.CatsListener
import com.example.catslist.viewmodels.CatsAdapter
import com.example.catslist.viewmodels.MainActivityViewModel

class MainActivity : AppCompatActivity() {
    private lateinit var adapter: CatsAdapter
    private lateinit var binding: ActivityMainBinding

    private val catsStorage: CatStorage
        get() = (applicationContext as App).catsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CatsAdapter()
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewWithCats.layoutManager = layoutManager
        binding.recyclerViewWithCats.adapter = adapter

        catsStorage.addListener(catsListener)
        repeat(5) { MainActivityViewModel().addCat() }
    }

    private val catsListener: CatsListener = {
        Log.v("catsListener", "catsListener!")
        adapter.catsList = it
    }
}