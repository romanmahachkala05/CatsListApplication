package com.example.catslist.tools

import android.util.Log
import com.example.catslist.models.Cat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

typealias CatsListener = (cats: List<Cat>) -> Unit

object CatStorage {

    private val tag = "CatStorage"
    private var cats = mutableListOf<Cat>()
    private val listeners = mutableListOf<CatsListener>()

    fun getCats(): List<Cat> {
        notifyChanges()
        return cats
    }

    fun addListener(listener: CatsListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CatsListener) {
        listeners.remove(listener)
    }

    private fun notifyChanges() {
        listeners.forEach { it.invoke(cats) }
    }

    fun getNewCatData() {
        Log.v(tag, "getNewCat")
        getCatData(object : DataReceivedCallback {
            override fun onDataReceived(catData: Cat?) {
                catData?.let {
                    if (cats.contains(catData)) {
                        Log.v(tag, "duplicate, adding new cat")
                        getCatData(this)
                    } else {
                        Log.v(tag, "adding item $it to catsList")
                        cats.add(it)
                        getCats()
                    }
                }
            }
        })
    }

    fun getCatData(callback: DataReceivedCallback) {
        Log.v(tag, "getCatData")
        GetCatRetrofit.getService().requestCatInfo().enqueue(object :
            Callback<List<Cat>> {
            override fun onResponse(call: Call<List<Cat>>, response: Response<List<Cat>>) {
                response.body()?.get(0)?.let {
                    callback.onDataReceived(it)
                }
            }

            override fun onFailure(call: Call<List<Cat>>, t: Throwable) {
                TODO("Not yet implemented")
            }
        }
        )
    }
}