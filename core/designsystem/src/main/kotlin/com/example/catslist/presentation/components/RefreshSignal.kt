package com.example.catslist.presentation.components

/**
 * What a screen's refresh request is doing, as [CatPullToRefresh] needs to hear it.
 *
 * [Failed] describes the request that most recently *finished*, not one in flight — an outcome
 * only becomes readable once the work stops, so there is deliberately no `Succeeded`: the
 * indicator infers it from a run that ended without failing.
 */
enum class RefreshSignal { Idle, Running, Failed }
