package com.example.catslist.presentation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App-level, cross-screen UI feedback (e.g. a Snackbar that must survive a
 * destination switch) — not a general event bus. A message that belongs to
 * one screen's own state/effects (a dialog, a screen-local error) should stay
 * on that screen's own `XxxEffect`, not be routed through here.
 */
interface UiNotifier {
    /**
     * One-shot messages, each delivered exactly once.
     *
     * Backed by a [Channel], which distributes rather than broadcasts: every element goes to a
     * single collector, chosen non-deterministically if more than one collects at a time — so
     * collect from one place only (`CatsNavDisplay`). Fanning out to several collectors would
     * need a `SharedFlow` instead.
     *
     * Capacity is [Channel.BUFFERED], so messages sent while nothing is collecting — across a
     * destination switch, say — are buffered and delivered once collection resumes; [showMessage]
     * suspends if that buffer fills rather than dropping.
     */
    val messages: Flow<UiText>

    suspend fun showMessage(message: UiText)
}

class DefaultUiNotifier @Inject constructor() : UiNotifier {
    private val channel = Channel<UiText>(Channel.BUFFERED)
    override val messages: Flow<UiText> = channel.receiveAsFlow()
    override suspend fun showMessage(message: UiText) = channel.send(message)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class UiNotifierModule {
    /** Scoped here rather than on [DefaultUiNotifier] — this is the binding everything injects. */
    @Binds
    @Singleton
    abstract fun bindUiNotifier(impl: DefaultUiNotifier): UiNotifier
}
