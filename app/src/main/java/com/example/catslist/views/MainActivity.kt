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
import com.example.catslist.room.*
import com.example.catslist.viewmodels.MainActivityViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val tag  = "MainActivity"
    private lateinit var adapter: CatsAdapter
    private lateinit var binding: ActivityMainBinding

    private val catsStorage: CatStorage
        get() = (applicationContext as App).catsService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = CatsAdapter(object: CatsActionsListener{
            override fun onAddToFavorites(cat: Cat) {
                TODO("Not yet implemented")
            }

            override fun onDownload(cat: Cat) {
                Log.v(tag, "onDownload()")
                MainActivityViewModel().downloadCatImage(applicationContext, cat.url, cat.id)
            }

        })
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerViewWithCats.layoutManager = layoutManager
        binding.recyclerViewWithCats.adapter = adapter
        binding.recyclerViewWithCats.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if(!recyclerView.canScrollVertically(1)) {
                    addCat()
                }
            }
        })

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

        catsStorage.addListener(catsListener)
        addCat()
    }

    private val catsListener: CatsListener = {
        Log.v("catsListener", "catsListener!")
        adapter.catsList = it
    }

    private fun addCat() {
        repeat(5) { MainActivityViewModel().addCat() }
    }
}