package com.example.catslist.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.catslist.databinding.ActivityMainBinding
import com.example.catslist.adapters.ViewPageAdapter
import com.google.android.material.tabs.TabLayoutMediator

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val fragmentsList = listOf(
        CatsListFragment.newInstance(),
        FavoriteCatsListFragment.newInstance()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initialize()
    }

    private fun initialize(){
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val viewPageAdapter = ViewPageAdapter(this, fragmentsList)
        viewPager.adapter = viewPageAdapter
        val fragmentsNames = listOf(
            "Infinite Cats",
            "Favorite Cats"
        )
        TabLayoutMediator(tabLayout, viewPager) { tab, pos ->
            tab.text = fragmentsNames[pos]
        }.attach()
    }
}