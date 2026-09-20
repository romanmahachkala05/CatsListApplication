package com.example.catslist.presentation

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/** UI text that does not know where it will be rendered. Only a Composable resolves it. */
sealed interface UiText {
    data class Raw(
        val value: String,
    ) : UiText
    data class Resource(
        @param:StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
}

@Suppress("SpreadOperator") // How `stringResource` takes format arguments; 0-2 of them here.
@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
}

/** For resolving outside composition, such as inside a `LaunchedEffect`. */
@Suppress("SpreadOperator")
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> context.getString(id, *args.toTypedArray())
}
