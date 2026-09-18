package com.example.catslist.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * Swaps `Dispatchers.Main` — which `viewModelScope` runs on and which has no
 * implementation on the JVM — for a test dispatcher, and puts it back afterwards.
 *
 * Defaults to [UnconfinedTestDispatcher] so coroutines a ViewModel starts in its
 * `init` run eagerly: by the time the constructor returns, the first collection
 * and the first load have already happened, and a test can assert on
 * `state.value` without advancing anything. Pass a `StandardTestDispatcher` in a
 * test that needs to control that ordering instead.
 */
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {

    override fun starting(description: Description) = Dispatchers.setMain(dispatcher)

    override fun finished(description: Description) = Dispatchers.resetMain()
}
