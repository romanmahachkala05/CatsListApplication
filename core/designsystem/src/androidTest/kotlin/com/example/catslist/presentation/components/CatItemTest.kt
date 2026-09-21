package com.example.catslist.presentation.components

import android.graphics.Bitmap
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.catslist.core.designsystem.R
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.testing.cat
import java.io.File
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The card's three image states are Coil's, so they are exercised through a real request — for
 * a local file, which fails and then succeeds exactly when this test says it does.
 */
@RunWith(AndroidJUnit4::class)
class CatItemTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    /**
     * A path of its own per test: Coil's memory cache lives on the singleton loader and
     * outlives any one test, so a shared name would serve the previous test's image.
     */
    private val imageFile = File(context.cacheDir, "cat-under-test-${SystemClock.elapsedRealtimeNanos()}.png")

    /** The app runs edge-to-edge, so the composable under test has to as well. */
    @Before
    fun holdTheClockAndStartWithNoImage() {
        composeRule.runOnUiThread { composeRule.activity.enableEdgeToEdge() }
        composeRule.mainClock.autoAdvance = false
        imageFile.delete()
    }

    @After
    fun cleanUp() {
        imageFile.delete()
    }

    @Test
    fun aCatWhoseImageWillNotLoadSaysSoRatherThanShimmeringOn() {
        showCatItem()

        awaitFailureStandIn(present = true)
    }

    /** The retry is per cat: a fresh painter, and so a fresh request, for this card alone. */
    @Test
    fun tappingTheStandInAsksForTheImageAgain() {
        showCatItem()
        awaitFailureStandIn(present = true)

        writeTheImage()
        composeRule.onNodeWithText(failedText).performClick()

        awaitFailureStandIn(present = false)
    }

    private fun showCatItem() {
        composeRule.setContent {
            CatsListTheme {
                CatItem(
                    cat = cat("1").copy(url = "file://${imageFile.absolutePath}"),
                    onFavoriteClick = {},
                    onDownloadClick = {},
                )
            }
        }
        composeRule.mainClock.advanceTimeByFrame()
    }

    private fun writeTheImage() {
        val bitmap = createBitmap(width = 1, height = 1)
        imageFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, QUALITY, it) }
    }

    /**
     * Frame by frame, with a real pause in between: the clock is held for the shimmer's sake,
     * but Coil's own work runs on the wall clock and has to be given room to land.
     */
    private fun awaitFailureStandIn(present: Boolean) {
        val deadline = SystemClock.elapsedRealtime() + TIMEOUT_MILLIS
        while (isShowing(failedText) != present) {
            check(SystemClock.elapsedRealtime() < deadline) {
                "The failure stand-in was ${if (present) "never shown" else "never dismissed"}"
            }
            composeRule.mainClock.advanceTimeByFrame()
            Thread.sleep(FRAME_MILLIS)
        }
    }

    private fun isShowing(text: String) = composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private val failedText get() = context.getString(R.string.common_cat_image_failed)

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
        const val FRAME_MILLIS = 16L
        const val QUALITY = 100
    }
}
