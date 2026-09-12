package com.example.catslist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.catslist.presentation.UiNotifier
import com.example.catslist.presentation.navigation.CatsNavDisplay
import com.example.catslist.presentation.theme.CatsListTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var uiNotifier: UiNotifier

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatsListTheme {
                CatsNavDisplay(notifier = uiNotifier)
            }
        }
    }
}
