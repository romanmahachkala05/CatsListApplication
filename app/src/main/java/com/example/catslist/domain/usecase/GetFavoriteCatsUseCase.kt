package com.example.catslist.domain.usecase

import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class GetFavoriteCatsUseCase @Inject constructor(
    private val repository: CatRepository,
) {
    operator fun invoke(): Flow<List<Cat>> = repository.favorites
}
