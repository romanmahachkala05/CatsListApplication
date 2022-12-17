package com.example.catslist.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.catslist.tools.CatStorage

class MainActivityViewModel: ViewModel() {

    fun addCat() {
        Log.v("MainActivityViewModel","addCat")
        CatStorage.getNewCatData()
    }

}