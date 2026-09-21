package com.example.catslist.presentation

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * App-level Snackbar messages: the ones that must survive a destination switch. Deliberately
 * narrow, not an event bus — anything screen-local stays on that screen's `XxxEffect`.
 */
interface SnackbarNotifier {
    /**
     * One-shot messages, each delivered exactly once. A [Channel] distributes rather than
     * broadcasts, so collect from one place only (`CatsNavDisplay`). Messages sent while
     * nothing collects are buffered, and [showMessage] suspends rather than dropping them.
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
    /** Scoped here, not on [DefaultSnackbarNotifier]: this is the binding everything injects. */
    @Binds
    @Singleton
    abstract fun bindSnackbarNotifier(impl: DefaultSnackbarNotifier): SnackbarNotifier
}
