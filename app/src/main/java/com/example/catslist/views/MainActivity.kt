package com.example.catslist.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.example.catslist.App
import com.example.catslist.databinding.ActivityMainBinding
import com.example.catslist.models.Cat
import com.example.catslist.tools.CatStorage
import com.example.catslist.tools.CatsListener
import com.example.catslist.adapters.CatsActionsListener
import com.example.catslist.adapters.CatsAdapter
import com.example.catslist.adapters.ViewPageAdapter
import com.example.catslist.room.*
import com.example.catslist.viewmodels.MainActivityViewModel
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tag  = "MainActivity"
    private lateinit var binding: ActivityMainBinding
    private val fragmentsList = listOf(
        CatsListFragment.newInstance(),
        FavoriteCatsListFragment.newInstance()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        //testing room database
        lateinit var catsDao: CatsDao
        val catsDb = Room.databaseBuilder(
            applicationContext,
            CatsDatabase::class.java, "cats_database"
        ).build()
        catsDao = catsDb.catsDao()
        fun testDB(){
            lifecycleScope.launch(Dispatchers.IO) {
                //catsDao.insertCat(CatDatabaseEntity("cat", "cat.com", 555, 555, true))
                val allCats = catsDao.getAllCats()
                Log.v(tag,"ALL CATS! $allCats")
            }
        }
        testDB()

        //ViewPage Adapter
        val viewPager = binding.viewPager
        val tabLayout = binding.tabLayout
        val viewPageAdapter = ViewPageAdapter(this, fragmentsList)
        viewPager.adapter = viewPageAdapter
        val fragmentsNames = listOf(
            "Infinite Cats",
            "Favorite Cats"
        )
        TabLayoutMediator(tabLayout, viewPager){
            tab, pos -> tab.text = fragmentsNames[pos]
        }.attach()


    }
}