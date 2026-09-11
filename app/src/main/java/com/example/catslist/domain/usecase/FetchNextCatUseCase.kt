package com.example.catslist.domain.usecase

import com.example.catslist.domain.repository.CatRepository
import javax.inject.Inject

class FetchNextCatUseCase @Inject constructor(
    private val repository: CatRepository
) {
    suspend operator fun invoke() = repository.fetchNextCat()
}
