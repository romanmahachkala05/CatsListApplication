package com.example.catslist.core.ui

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
