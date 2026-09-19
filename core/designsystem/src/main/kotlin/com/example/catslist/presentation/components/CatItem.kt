package com.example.catslist.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.example.catslist.core.designsystem.R
import com.example.catslist.domain.model.Cat
import com.example.catslist.presentation.theme.CatsListTheme

/** One cat card: image in a notched frame, with the action icons sitting in the notch. */
@Composable
fun CatItem(
    cat: Cat,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = remember {
        CatCardShape(CARD_CORNER, NOTCH_WIDTH, NOTCH_HEIGHT, NOTCH_CORNER, NOTCH_SWEEP)
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_MARGIN_HORIZONTAL, vertical = CARD_MARGIN_VERTICAL),
    ) {
        AsyncImage(
            model = cat.url,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
                .clip(shape)
                // The same shape for the outline as for the clip: a border drawn from a second
                // shape would need the notch measurements repeated, and could drift from them.
                .border(BORDER_WIDTH, MaterialTheme.colorScheme.outline, shape),
        )
        CatActions(
            isFavorite = cat.isFavorite,
            onFavoriteClick = onFavoriteClick,
            onDownloadClick = onDownloadClick,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/**
 * Sized from the very constants the notch is cut with, so the row cannot outgrow the gap it sits
 * in. The icons are on the app background here rather than on the image, so they take their
 * colour from the theme — the white tint this used to hardcode was only legible because it sat
 * on a photo.
 */
@Composable
private fun CatActions(
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onDownloadClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val favoriteTint by animateColorAsState(
        targetValue = if (isFavorite) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        label = "favoriteTint",
    )
    Row(
        modifier = modifier.width(NOTCH_WIDTH).height(NOTCH_HEIGHT),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFavoriteClick) {
            Icon(
                painter = painterResource(
                    if (isFavorite) R.drawable.ic_favorite_filled else R.drawable.ic_favorite_outline,
                ),
                contentDescription = stringResource(R.string.common_cd_favorite_cat),
                tint = favoriteTint,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
        IconButton(onClick = onDownloadClick) {
            Icon(
                painter = painterResource(R.drawable.ic_download),
                contentDescription = stringResource(R.string.common_cd_download_cat),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(ICON_SIZE),
            )
        }
    }
}

private val CARD_MARGIN_HORIZONTAL = 16.dp
private val CARD_MARGIN_VERTICAL = 8.dp
private val CARD_CORNER = 20.dp
private val IMAGE_HEIGHT = 300.dp
private val BORDER_WIDTH = 3.dp
private val ICON_SIZE = 24.dp

/** Two 48dp touch targets side by side, plus the breathing room around them. */
private val NOTCH_WIDTH = 104.dp
private val NOTCH_HEIGHT = 52.dp
private val NOTCH_CORNER = 20.dp
private val NOTCH_SWEEP = 16.dp

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
