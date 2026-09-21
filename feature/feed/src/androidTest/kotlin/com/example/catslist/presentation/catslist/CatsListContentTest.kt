package com.example.catslist.presentation.catslist

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.core.designsystem.R as designsystemR
import com.example.catslist.domain.model.Cat
import com.example.catslist.feature.feed.R
import com.example.catslist.presentation.components.CAT_LIST_PLACEHOLDER_TAG
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import com.google.common.truth.Truth.assertThat
import java.io.IOException
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The feed's branches are decided by `LazyPagingItems.loadState` (ADR-0024), which only exists
 * inside composition — so these are the only tests that can reach them at all.
 */
@RunWith(AndroidJUnit4::class)
class CatsListContentTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * The skeleton and the card shimmer forever, and an animation that never ends never lets
     * the test clock go idle. Driving the clock by hand is what keeps assertions from hanging.
     */
    @Before
    fun holdTheClock() {
        composeRule.mainClock.autoAdvance = false
    }

    /**
     * The regression test for the endless skeleton: Paging never reports
     * `endOfPaginationReached` on a refresh, so a feed that came back empty looks exactly like
     * one still loading unless the screen reads the loading flags themselves.
     */
    @Test
    fun anEmptyFeedSaysSoInsteadOfShimmeringForever() {
        showContent(cats = emptyList(), states = settled())

        composeRule.onNodeWithText(string(R.string.catslist_empty_message)).assertIsDisplayed()
        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertDoesNotExist()
    }

    @Test
    fun aRefreshStillRunningShowsTheSkeleton() {
        showContent(cats = emptyList(), states = settled(refresh = LoadState.Loading))

        composeRule.onNodeWithTag(CAT_LIST_PLACEHOLDER_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.catslist_empty_message)).assertDoesNotExist()
    }

    @Test
    fun aFailedRefreshWithNoCatsOffersARetry() {
        showContent(cats = emptyList(), states = settled(refresh = LoadState.Error(IOException("offline"))))

        composeRule.onNodeWithText(string(R.string.catslist_error_loading_cats)).assertIsDisplayed()
        composeRule.onNodeWithText(string(designsystemR.string.common_action_retry)).assertIsDisplayed()
    }

    /** The paged cat carries no favorite status; the screen overlays it (ADR-0024). */
    @Test
    fun theFavoriteOverlayReachesTheEventTheCardSends() {
        val events = mutableListOf<CatsListEvent>()
        showContent(
            cats = listOf(cat("1"), cat("2")),
            states = settled(),
            state = CatsListState(favoriteIds = persistentSetOf("2")),
            onEvent = { events += it },
        )

        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_favorite_cat))[1]
            .performClick()

        assertThat(events).containsExactly(CatsListEvent.ToggleFavorite(cat("2").copy(isFavorite = true)))
    }

    @Test
    fun aBrokenFavoritesStreamIsAnnouncedAboveTheCatsRatherThanInsteadOfThem() {
        showContent(
            cats = listOf(cat("1")),
            states = settled(),
            state = CatsListState(favoritesStatus = CatsListFavoritesStatus.Unavailable),
        )

        composeRule.onNodeWithText(string(R.string.catslist_error_favorites_unavailable)).assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription(string(designsystemR.string.common_cd_favorite_cat))[0]
            .assertIsDisplayed()
    }

    @Test
    fun aFailedNextPageIsAFooterUnderTheCatsThatAreUp() {
        showContent(
            cats = listOf(cat("1")),
            states = settled(append = LoadState.Error(IOException("offline"))),
        )

        composeRule.onNodeWithText(string(R.string.catslist_error_loading_cats)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.catslist_action_retry)).assertIsDisplayed()
    }

    private fun showContent(
        cats: List<Cat>,
        states: LoadStates,
        state: CatsListState = CatsListState(),
        onEvent: (CatsListEvent) -> Unit = {},
    ) {
        composeRule.setContent {
            CatsListTheme {
                CatsListContent(
                    pagingItems = flowOf(PagingData.from(cats, states)).collectAsLazyPagingItems(),
                    onEvent = onEvent,
                    state = state,
                )
            }
        }
        // Enough for the first page to reach composition, and past the skeleton's own floor.
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
    }

    /**
     * What Paging itself produces once a load is done: `Complete` is only ever set on prepend
     * and append, never on refresh — which is the whole reason the empty feed had nowhere to go.
     */
    private fun settled(
        refresh: LoadState = LoadState.NotLoading(endOfPaginationReached = false),
        append: LoadState = LoadState.NotLoading(endOfPaginationReached = true),
    ) = LoadStates(
        refresh = refresh,
        prepend = LoadState.NotLoading(endOfPaginationReached = true),
        append = append,
    )

    private fun string(id: Int) = context.getString(id)

    private companion object {
        const val SETTLE_MILLIS = 1_000L
    }
}
