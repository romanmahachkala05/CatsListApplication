package com.example.catslist.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.GlideImage
import com.example.catslist.R
import com.example.catslist.core.ui.theme.CatsListTheme
import com.example.catslist.domain.model.Cat

/** One cat card: image + download button + favorite toggle. Shared by both screens. */
@Composable
fun CatItem(
    cat: Cat,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        GlideImage(
            model = cat.url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp),
        )
        IconButton(
            onClick = onDownloadClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.outline_file_download_white_48),
                contentDescription = stringResource(R.string.cd_download_cat),
                tint = Color.White,
            )
        }
        IconButton(
            onClick = onFavoriteClick,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 56.dp, bottom = 8.dp),
        ) {
            Icon(
                painter = painterResource(
                    if (cat.isFavorite) R.drawable.ic_star_filled else R.drawable.ic_star_empty
                ),
                contentDescription = stringResource(R.string.cd_favorite_cat),
                tint = if (cat.isFavorite) MaterialTheme.colorScheme.primary else Color.White,
            )
        }
    }
}

@Preview(name = "Not favorite", showBackground = true)
@Composable
private fun CatItemPreview() {
    CatsListTheme {
        CatItem(
            cat = Cat(id = "1", url = "", width = 300, height = 300, isFavorite = false),
            onFavoriteClick = {},
            onDownloadClick = {},
        )
    }
}

@Preview(name = "Favorite", showBackground = true)
@Composable
private fun CatItemFavoritePreview() {
    CatsListTheme {
        CatItem(
            cat = Cat(id = "1", url = "", width = 300, height = 300, isFavorite = true),
            onFavoriteClick = {},
            onDownloadClick = {},
        )
    }
}
