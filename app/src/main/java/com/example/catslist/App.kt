package com.example.catslist

import android.app.Application
import com.example.catslist.tools.CatStorage

class App: Application() {
    val catsService = CatStorage
}