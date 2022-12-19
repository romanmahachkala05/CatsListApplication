package com.example.catslist.tools

import android.util.Log
import com.example.catslist.models.Cat
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

typealias CatsListener = (cats: List<Cat>) -> Unit

object CatStorage {

    private val tag = "CatStorage"
    var cats = mutableListOf<Cat>()
    var favoriteCats = mutableListOf<Cat>()
    private val listeners = mutableListOf<CatsListener>()
    private val favListeners = mutableListOf<CatsListener>()

    fun getFavCats(): List<Cat> {
        notifyChanges()
        return favoriteCats
    }

    fun addFavListener(listener: CatsListener) {
        favListeners.add(listener)
    }

    fun removeFavListener(listener: CatsListener) {
        favListeners.remove(listener)
    }

    fun notifyFavChanges() {
        Log.v(tag, "notifyFavChanges")
        favListeners.forEach { it.invoke(favoriteCats) }
    }

    fun getCatslist(): List<Cat> {
        notifyChanges()
        return cats
    }

    fun addListener(listener: CatsListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: CatsListener) {
        listeners.remove(listener)
    }

    fun notifyChanges() {
        listeners.forEach { it.invoke(cats) }
    }

    fun getNewCatData() {
        getCatData(object : DataReceivedCallback {
            override fun onDataReceived(catData: Cat?) {
                catData?.let {
                    if (cats.contains(catData)) {
                        getCatData(this)
                    } else {
                        cats.add(it)
                        getCatslist()
                    }
                }
            }
        })
    }

    fun getCatData(callback: DataReceivedCallback) {
        GetCatRetrofit.getService().requestCatInfo().enqueue(object :
            Callback<List<Cat>> {
            override fun onResponse(call: Call<List<Cat>>, response: Response<List<Cat>>) {
                response.body()?.get(0)?.let {
                    callback.onDataReceived(it)
                }
            }

            override fun onFailure(call: Call<List<Cat>>, t: Throwable) {
                Log.e(tag, "error loading cat: ${t.message}")
            }
        }
        )
    }
}