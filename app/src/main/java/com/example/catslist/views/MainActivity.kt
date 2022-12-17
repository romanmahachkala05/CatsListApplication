package com.example.catslist.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.catslist.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private val catsListener: CatsListener = {
        Log.v("catsListener", "catsListener!")
        adapter.catsList = it
    }
}