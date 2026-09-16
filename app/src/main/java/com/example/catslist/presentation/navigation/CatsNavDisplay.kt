package com.example.catslist.presentation.navigation

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberDecoratedNavEntries
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.catslist.R
import com.example.catslist.presentation.SnackbarNotifier
import com.example.catslist.presentation.catslist.CatsListNavKey
import com.example.catslist.presentation.catslist.CatsListScreen
import com.example.catslist.presentation.favoritecats.FavoriteCatsNavKey
import com.example.catslist.presentation.favoritecats.FavoriteCatsScreen
import com.example.catslist.presentation.resolve

/**
 * [CatsListNavKey] and [FavoriteCatsNavKey] are peer top-level destinations switched via the
 * floating bottom bar, each with its own back stack — so switching tabs shows that screen's own
 * preserved scroll position and ViewModel instead of resetting them, the way a single shared,
 * cleared-and-replaced back stack would. Mirrors Navigation 3's own `MultipleBackStackSample`.
 * Each screen's ViewModel is still created lazily by its own `hiltViewModel()` default — this
 * composable never needs to know either one's concrete type, since Snackbar delivery goes
 * through the shared [com.example.catslist.presentation.SnackbarNotifier] instead.
 */
@Composable
fun CatsNavDisplay(notifier: SnackbarNotifier, modifier: Modifier = Modifier) {
    var selected by remember { mutableStateOf<NavKey>(CatsListNavKey) }
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(notifier) {
        notifier.messages.collect { message ->
            snackbarHostState.showSnackbar(message.resolve(context))
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            FloatingBottomBar(selected = selected, onSelect = { selected = it })
        },
    ) { innerPadding ->
        // Deliberately not padding the content: the bar floats over it, so cats run full-bleed
        // underneath. The insets go to each list as contentPadding so items still scroll clear.
        //
        // Each tab's rememberDecoratedNavEntries call runs on every recomposition regardless of
        // which tab is selected, so its SaveableStateHolder/ViewModelStore decorators — and
        // whatever they're holding — stay alive the whole time. NavDisplay below only ever
        // renders the entries for the tab that's currently selected.
        val catsListBackStack = rememberNavBackStack(CatsListNavKey)
        val catsListEntries = rememberDecoratedNavEntries(
            backStack = catsListBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<CatsListNavKey> { CatsListScreen(contentPadding = innerPadding) }
            },
        )

        val favoriteCatsBackStack = rememberNavBackStack(FavoriteCatsNavKey)
        val favoriteCatsEntries = rememberDecoratedNavEntries(
            backStack = favoriteCatsBackStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<FavoriteCatsNavKey> { FavoriteCatsScreen(contentPadding = innerPadding) }
            },
        )

        NavDisplay(
            entries = if (selected == CatsListNavKey) catsListEntries else favoriteCatsEntries,
            onBack = {
                val backStack = if (selected == CatsListNavKey) catsListBackStack else favoriteCatsBackStack
                backStack.removeLastOrNull()
            },
        )
    }
}

/**
 * Material3's [NavigationBar] — which brings the selection indicator, its animations and the
 * `selectableGroup()` semantics with it — shaped into a floating pill instead of spanning the
 * screen edge to edge. `Modifier.shadow` clips to the shape, so the bar paints its own container
 * and needs no wrapping Surface. `IntrinsicSize.Min` keeps it hugging its two items, since
 * [NavigationBarItem] otherwise weights itself across the full available width.
 */
@Composable
private fun FloatingBottomBar(
    selected: NavKey?,
    onSelect: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(
        // The bottomBar slot lays out from the start edge, so centre the pill within it.
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .wrapContentWidth()
            .width(IntrinsicSize.Min)
            .shadow(6.dp, BAR_SHAPE),
        // The Scaffold slot already handles system bars; NavigationBar's own insets would double them.
        windowInsets = WindowInsets(0, 0, 0, 0),
    ) {
        NavigationBarItem(
            selected = selected == CatsListNavKey,
            onClick = { onSelect(CatsListNavKey) },
            // The label names the destination; a description here would announce it twice.
            icon = {
                Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, modifier = Modifier.size(ICON_SIZE))
            },
            label = { Text(stringResource(R.string.catslist_nav_label), fontWeight = FontWeight.Normal) },
            colors = navigationBarItemColors(),
        )
        NavigationBarItem(
            selected = selected == FavoriteCatsNavKey,
            onClick = { onSelect(FavoriteCatsNavKey) },
            icon = { Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(ICON_SIZE)) },
            label = { Text(stringResource(R.string.favoritecats_nav_label), fontWeight = FontWeight.Normal) },
            colors = navigationBarItemColors(),
        )
    }
}

private val BAR_SHAPE = RoundedCornerShape(50)

/** Labels stay the same plain on-surface colour in both states; only the icon reflects selection. */
@Composable
private fun navigationBarItemColors() = NavigationBarItemDefaults.colors(
    selectedTextColor = MaterialTheme.colorScheme.onSurface,
    unselectedTextColor = MaterialTheme.colorScheme.onSurface,
)

/**
 * 24.dp is [Icon]'s default; the M3 active-indicator behind it is a fixed 64x32.dp,
 * so much past this looks cramped.
 */
private val ICON_SIZE = 48.dp
