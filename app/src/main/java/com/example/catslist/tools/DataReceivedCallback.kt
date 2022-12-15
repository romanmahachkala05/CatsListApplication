package com.example.catslist.tools

import com.example.catslist.models.Cat

interface DataReceivedCallback {
    fun onDataReceived(catData: Cat?)
}