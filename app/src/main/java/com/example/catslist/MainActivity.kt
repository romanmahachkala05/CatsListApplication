package com.example.catslist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.example.catslist.presentation.theme.CatsListTheme
import com.example.catslist.presentation.catslist.CatsListScreen
import com.example.catslist.presentation.favoritecats.FavoriteCatsScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Two peer tabs, no push/pop navigation between them — a plain [TabRow] over
 * local selection state covers this app's real navigation needs, so there's
 * no Navigation 3 back stack to wire up here (ARCHITECTURE.md: delete a
 * section that doesn't apply). Add one if a pushed screen (e.g. cat detail)
 * shows up later.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatsListTheme {
                CatsListApp()
            }
        }
    }
}

@Composable
private fun CatsListApp() {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabTitles = listOf(
        stringResource(R.string.tab_infinite_cats),
        stringResource(R.string.tab_favorite_cats),
    )

    Scaffold(
        topBar = {
            TabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) },
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            0 -> CatsListScreen(modifier = Modifier.padding(innerPadding))
            1 -> FavoriteCatsScreen(modifier = Modifier.padding(innerPadding))
        }
    }
}
