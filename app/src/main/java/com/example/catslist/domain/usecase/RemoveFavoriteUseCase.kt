package com.example.catslist.domain.usecase

import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject

class RemoveFavoriteUseCase @Inject constructor(
    private val repository: CatRepository,
) {
    suspend operator fun invoke(cat: Cat) = repository.removeFavorite(cat)
}
