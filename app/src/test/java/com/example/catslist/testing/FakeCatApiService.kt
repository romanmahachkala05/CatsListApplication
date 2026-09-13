package com.example.catslist.testing

import com.example.catslist.data.remote.CatApiService
import com.example.catslist.data.remote.CatDto

/** Hands out queued responses in order, and records the paging limit it was called with. */
class FakeCatApiService : CatApiService {

    private val responses = ArrayDeque<List<CatDto>>()

    /** When set, every request fails with it — the network error the repository lets propagate. */
    var error: Throwable? = null

    var lastRequestedLimit: Int? = null
        private set

    fun enqueueResponse(vararg cats: CatDto) = responses.addLast(cats.toList())

    override suspend fun requestCatInfo(limit: Int): List<CatDto> {
        lastRequestedLimit = limit
        error?.let { throw it }
        return responses.removeFirstOrNull().orEmpty()
    }
}
