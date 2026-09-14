package com.example.catslist.presentation

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

/**
 * UI text that doesn't know where it will be rendered. ViewModels/StateHolders
 * put one of these in state; only a Composable resolves it to an actual String.
 */
sealed interface UiText {
    data class Raw(
        val value: String,
    ) : UiText
    data class Resource(
        @StringRes val id: Int,
        val args: List<Any> = emptyList(),
    ) : UiText
    data class Plural(
        @PluralsRes val id: Int,
        val count: Int,
    ) : UiText
}

@Composable
fun UiText.resolve(): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> stringResource(id, *args.toTypedArray())
    is UiText.Plural -> pluralStringResource(id, count, count)
}

/** For resolving outside composition, e.g. inside a [LaunchedEffect] collecting a one-shot effect. */
fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Raw -> value
    is UiText.Resource -> context.getString(id, *args.toTypedArray())
    is UiText.Plural -> context.resources.getQuantityString(id, count, count)
}
