package com.example.catslist.presentation.favoritecats

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.core.designsystem.R as designsystemR
import com.example.catslist.feature.favorites.R
import com.example.catslist.presentation.UiText
import com.example.catslist.presentation.components.CAT_LIST_PLACEHOLDER_TAG
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import kotlinx.collections.immutable.persistentListOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * One test per `FavoriteCatsUiStatus`: the ViewModel tests prove which status the screen ends
 * up in, and these prove what that status puts on screen.
 */
@RunWith(AndroidJUnit4::class)
class FavoriteCatsContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The skeleton and the cards shimmer forever, and an animation that never ends never lets
     * the test clock go idle. Driving the clock by hand is what keeps assertions from hanging.
     */
    @Before
    fun holdTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    @Test
    fun contentPutsEveryFavoriteOnScreen() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats))

        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_favorite_cat))
            .assertCountEquals(2)
        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun anEmptyListSaysSoRatherThanShowingNothing() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Empty))

        composeRule.onNodeWithText(string(R.string.favoritecats_empty_message)).assertIsDisplayed()
    }

    @Test
    fun loadingShowsTheSkeleton() {
        showContent(FavoriteCatsState(status = FavoriteCatsUiStatus.Loading))

        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.favoritecats_empty_message)).assertDoesNotExist()
    }

    /** The empty list and a failure to read it are not the same thing to the user. */
    @Test
    fun anErrorShowsItsOwnMessageAndAskingAgainIsAnEvent() {
        val events = mutableListOf<FavoriteCatsEvent>()
        showContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Error(errorMessage)),
            onEvent = { events += it },
        )

        composeRule.onNodeWithText(string(R.string.favoritecats_error_loading_favorites)).assertIsDisplayed()
        composeRule.onNodeWithText(string(designsystemR.string.common_action_retry)).performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.Retry)
    }

    /** The same star that favorites a cat on the feed takes it off this screen. */
    @Test
    fun theStarOnACardRemovesThatCat() {
        val events = mutableListOf<FavoriteCatsEvent>()
        showContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats),
            onEvent = { events += it },
        )

        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_favorite_cat))[1]
            .performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.RemoveFavorite(cat("2", isFavorite = true)))
    }

    @Test
    fun theDownloadOnACardAsksForThatCat() {
        val events = mutableListOf<FavoriteCatsEvent>()
        showContent(
            state = FavoriteCatsState(status = FavoriteCatsUiStatus.Content, cats = twoCats),
            onEvent = { events += it },
        )

        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_download_cat))[0]
            .performClick()

        assertThat(events).containsExactly(FavoriteCatsEvent.Download(cat("1", isFavorite = true)))
    }

    private fun showContent(state: FavoriteCatsState, onEvent: (FavoriteCatsEvent) -> Unit = {}) {
        composeRule.setContent {
            CatsListTheme { FavoriteCatsContent(state = state, onEvent = onEvent) }
        }
        // Enough for the first frame, and past any hold a status change animates through.
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    private fun string(id: Int) = context.getString(id)

    private companion object {
        val twoCats = persistentListOf(cat("1", isFavorite = true), cat("2", isFavorite = true))
        val errorMessage = UiText.Resource(R.string.favoritecats_error_loading_favorites)
        const val SETTLE_MILLIS = 1_000L
    }
}
