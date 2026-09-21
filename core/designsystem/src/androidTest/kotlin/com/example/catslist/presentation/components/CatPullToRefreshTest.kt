package com.example.catslist.presentation.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.core.designsystem.R
import com.example.catslist.presentation.theme.CatsListTheme
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The indicator reports the outcome of a refresh the user asked for, so every phase it shows
 * is a function of both the [RefreshSignal] and whether a pull happened — which only a gesture
 * against the real component can exercise.
 */
@RunWith(AndroidJUnit4::class)
class CatPullToRefreshTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val signal = mutableStateOf(RefreshSignal.Idle)
    private var refreshes = 0

    /** Each phase is held deliberately, so the clock is the thing under test as much as the state. */
    @Before
    fun holdTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun aPullAsksForARefresh() {
        showPullToRefresh()

        pull()

        assertThat(refreshes).isEqualTo(1)
    }

    /**
     * Paging refreshes on its own at launch. The indicator has to stay out of the way for one
     * the user never asked for, which is what `pullRequested` is there to decide.
     */
    @Test
    fun aRefreshNobodyPulledForIsNotAnnounced() {
        showPullToRefresh()

        signalNow(RefreshSignal.Running)

        composeRule.onNodeWithContentDescription(string(R.string.common_cd_refreshing)).assertDoesNotExist()
    }

    /**
     * The signal is set a full settle later than the pull on purpose: the refresh it kicked
     * off reports `Running` when it is good and ready, and the indicator owes the user the
     * sequence either way.
     */
    @Test
    fun aPulledRefreshShowsTheSpinnerWhileItRuns() {
        showPullToRefresh()
        pull()

        signalNow(RefreshSignal.Running)

        composeRule.onNodeWithContentDescription(string(R.string.common_cd_refreshing)).assertIsDisplayed()
    }

    @Test
    fun aRefreshThatWorkedShowsTheTickBeforeRetracting() {
        showPullToRefresh()
        pull()
        signalNow(RefreshSignal.Running)

        signalNow(RefreshSignal.Idle, advanceBy = PHASE_HOLD)

        composeRule.onNodeWithContentDescription(string(R.string.common_cd_refresh_succeeded)).assertIsDisplayed()
        // And it is gone again once the hold is over, rather than parked on screen.
        composeRule.mainClock.advanceTimeBy(PHASE_HOLD + SETTLE_MILLIS)
        composeRule.onNodeWithContentDescription(string(R.string.common_cd_refresh_succeeded)).assertDoesNotExist()
    }

    /** A failure the user pulled for is its own phase: the tick would be a lie. */
    @Test
    fun aRefreshThatFailedShowsTheCross() {
        showPullToRefresh()
        pull()
        signalNow(RefreshSignal.Running)

        signalNow(RefreshSignal.Failed, advanceBy = PHASE_HOLD)

        composeRule.onNodeWithContentDescription(string(R.string.common_cd_refresh_failed)).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(string(R.string.common_cd_refresh_succeeded)).assertDoesNotExist()
    }

    private fun showPullToRefresh() {
        composeRule.setContent {
            CatsListTheme {
                CatPullToRefresh(
                    signal = signal.value,
                    onRefresh = { refreshes++ },
                    modifier = Modifier.fillMaxSize(),
                ) {
                    // Scrollable on purpose: the pull is delivered through nested scroll.
                    LazyColumn(modifier = Modifier.fillMaxSize().testTag(CONTENT_TAG)) {
                        items((1..20).toList()) { Text(text = "cat $it") }
                    }
                }
            }
        }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun pull() {
        composeRule.onNodeWithTag(CONTENT_TAG).performTouchInput { swipeDown() }
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun signalNow(next: RefreshSignal, advanceBy: Long = SETTLE_MILLIS) {
        composeRule.runOnUiThread { signal.value = next }
        composeRule.mainClock.advanceTimeBy(advanceBy)
    }

    private fun string(id: Int) = context.getString(id)

    private companion object {
        const val CONTENT_TAG = "content"

        /** Comfortably past the component's own 300ms minimum per phase. */
        const val PHASE_HOLD = 400L
        const val SETTLE_MILLIS = 1_000L
    }
}
