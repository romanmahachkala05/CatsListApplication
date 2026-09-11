package com.example.catslist.domain.usecase

import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCatFeedUseCase @Inject constructor(
    private val repository: CatRepository
) {
    operator fun invoke(): Flow<List<Cat>> = repository.feed
}
