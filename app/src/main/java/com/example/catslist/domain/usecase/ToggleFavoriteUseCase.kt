package com.example.catslist.domain.usecase

import com.example.catslist.domain.model.Cat
import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject

class ToggleFavoriteUseCase @Inject constructor(
    private val repository: CatRepository,
) {
    suspend operator fun invoke(cat: Cat) = repository.toggleFavorite(cat)
}
