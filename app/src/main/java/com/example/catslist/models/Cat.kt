package com.example.catslist.models

import android.view.inspector.IntFlagMapping

data class Cat(
    var id: String,
    var url: String,
    var width: Int,
    var height: Int,
    var favorite: Boolean
)

