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
sealed interface TextSource {
    data class Raw(val value: String) : TextSource
    data class Res(@StringRes val id: Int, val args: List<Any> = emptyList()) : TextSource
    data class Plural(@PluralsRes val id: Int, val count: Int) : TextSource
}

@Composable
fun TextSource.resolve(): String = when (this) {
    is TextSource.Raw -> value
    is TextSource.Res -> stringResource(id, *args.toTypedArray())
    is TextSource.Plural -> pluralStringResource(id, count, count)
}

/** For resolving outside composition, e.g. inside a [LaunchedEffect] collecting a one-shot effect. */
fun TextSource.resolve(context: Context): String = when (this) {
    is TextSource.Raw -> value
    is TextSource.Res -> context.getString(id, *args.toTypedArray())
    is TextSource.Plural -> context.resources.getQuantityString(id, count, count)
}
