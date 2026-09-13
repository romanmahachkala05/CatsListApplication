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
 * App-level Snackbar messages — the ones that must survive a destination switch, so they can't
 * live on a screen that gets disposed. Deliberately narrow, not a general event bus: anything
 * belonging to one screen's own state or effects (a dialog, a screen-local error) stays on that
 * screen's `XxxEffect`. Other kinds of app-level surface (a toast, a dialog host) get their own
 * collaborator rather than widening this one.
 */
interface SnackbarNotifier {
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

class DefaultSnackbarNotifier @Inject constructor() : SnackbarNotifier {
    private val channel = Channel<UiText>(Channel.BUFFERED)
    override val messages: Flow<UiText> = channel.receiveAsFlow()
    override suspend fun showMessage(message: UiText) = channel.send(message)
}

@Module
@InstallIn(SingletonComponent::class)
abstract class SnackbarNotifierModule {
    /** Scoped here rather than on [DefaultSnackbarNotifier] — this is the binding everything injects. */
    @Binds
    @Singleton
    abstract fun bindSnackbarNotifier(impl: DefaultSnackbarNotifier): SnackbarNotifier
}
