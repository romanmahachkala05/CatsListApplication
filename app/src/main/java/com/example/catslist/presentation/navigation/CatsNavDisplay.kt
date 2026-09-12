package com.example.catslist.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.catslist.R
import com.example.catslist.presentation.catslist.CatsListNavKey
import com.example.catslist.presentation.catslist.CatsListScreen
import com.example.catslist.presentation.favoritecats.FavoriteCatsNavKey
import com.example.catslist.presentation.favoritecats.FavoriteCatsScreen
import com.example.catslist.presentation.UiNotifier
import com.example.catslist.presentation.resolve

/**
 * The app's one back stack — [CatsListNavKey] and [FavoriteCatsNavKey] are peer
 * top-level destinations switched via the floating bottom bar (replace, not
 * push), leaving room for a real pushed screen (e.g. cat detail) later. Each
 * screen's ViewModel is still created lazily by its own `hiltViewModel()`
 * default — this composable never needs to know either one's concrete type,
 * since Snackbar delivery goes through the shared [com.example.catslist.presentation.UiNotifier] instead.
 */
@Composable
fun CatsNavDisplay(notifier: UiNotifier, modifier: Modifier = Modifier) {
    val backStack = rememberNavBackStack(CatsListNavKey)
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
            FloatingBottomBar(
                selected = backStack.lastOrNull(),
                onSelect = { key ->
                    if (backStack.lastOrNull() != key) {
                        backStack.clear()
                        backStack.add(key)
                    }
                },
            )
        },
    ) { innerPadding ->
        NavDisplay(
            backStack = backStack,
            modifier = Modifier.padding(innerPadding),
            entryDecorators = listOf(rememberSaveableStateHolderNavEntryDecorator()),
            entryProvider = entryProvider {
                entry<CatsListNavKey> { CatsListScreen() }
                entry<FavoriteCatsNavKey> { FavoriteCatsScreen() }
            },
        )
    }
}

@Composable
private fun FloatingBottomBar(selected: NavKey?, onSelect: (NavKey) -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.padding(16.dp),
        shape = RoundedCornerShape(50),
        tonalElevation = 3.dp,
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .width(IntrinsicSize.Min)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NavigationBarItem(
                selected = selected == CatsListNavKey,
                onClick = { onSelect(CatsListNavKey) },
                icon = {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = stringResource(R.string.catslist_tab_title))
                },
            )
            NavigationBarItem(
                selected = selected == FavoriteCatsNavKey,
                onClick = { onSelect(FavoriteCatsNavKey) },
                icon = {
                    Icon(Icons.Default.Star, contentDescription = stringResource(R.string.favoritecats_tab_title))
                },
            )
        }
    }
}
