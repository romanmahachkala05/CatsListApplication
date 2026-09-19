package com.example.catslist.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
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
    // Every card fetches its own photo, so one cat can fail while the page around it loaded
    // fine. Bumping `attempt` gives the failed one a fresh painter, and so a fresh request.
    var attempt by remember(cat.url) { mutableIntStateOf(0) }
    var status by remember(cat.url, attempt) { mutableStateOf(ImageStatus.Loading) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_MARGIN_HORIZONTAL, vertical = CARD_MARGIN_VERTICAL),
    ) {
        key(attempt) {
            AsyncImage(
                model = cat.url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onState = { status = it.toImageStatus() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IMAGE_HEIGHT)
                    .clip(CAT_CARD_SHAPE)
                    // The same shape for the outline as for the clip: a border drawn from a
                    // second shape would need the notch measurements repeated, and could drift.
                    .border(BORDER_WIDTH, MaterialTheme.colorScheme.outline, CAT_CARD_SHAPE),
            )
        }
        // Drawn over the image — which is blank in both of these states — but under the actions.
        when (status) {
            // Composed only while loading, so the infinite animation stops costing frames once
            // the photo is up.
            ImageStatus.Loading -> Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CAT_CARD_SHAPE)
                    .background(shimmerBrush()),
            )
            ImageStatus.Loaded -> Unit
            ImageStatus.Failed -> CatImageError(
                onRetry = { attempt++ },
                modifier = Modifier.matchParentSize(),
            )
        }
        CatActions(
            isFavorite = cat.isFavorite,
            onFavoriteClick = onFavoriteClick,
            onDownloadClick = onDownloadClick,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
    }
}

/**
 * The skeleton shown before there is any cat to frame: the whole card is the shimmer, with no
 * outline. That is deliberately *not* how [CatItem] looks while its photo loads — there, the
 * frame is already drawn and only its inside shimmers. A framed card means "this cat exists,
 * its picture is coming"; an unframed shimmer means "we don't have a cat yet".
 *
 * It keeps [CatItem]'s footprint and shape, so nothing shifts when real cats replace it.
 */
@Composable
fun CatItemPlaceholder(modifier: Modifier = Modifier) {
    val shimmer = shimmerBrush()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = CARD_MARGIN_HORIZONTAL, vertical = CARD_MARGIN_VERTICAL),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(IMAGE_HEIGHT)
                .clip(CAT_CARD_SHAPE)
                .background(shimmer),
        )
    }
}

/**
 * Shown in place of a cat whose own image request failed, while the page around it loaded fine.
 * Tapping retries just this one — without that the card stays broken for as long as the list
 * keeps it alive, and a pull-to-refresh of the whole feed is a heavy way to recover one photo.
 */
@Composable
private fun CatImageError(onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(CAT_CARD_SHAPE)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClickLabel = stringResource(R.string.common_cd_retry_cat_image), onClick = onRetry),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_image_failed),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(ERROR_ICON_SIZE),
        )
        Text(
            text = stringResource(R.string.common_cat_image_failed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

private enum class ImageStatus { Loading, Loaded, Failed }

private fun AsyncImagePainter.State.toImageStatus(): ImageStatus = when (this) {
    is AsyncImagePainter.State.Loading -> ImageStatus.Loading
    is AsyncImagePainter.State.Error -> ImageStatus.Failed
    // Empty means no request was made at all — nothing is coming, so nothing should shimmer.
    is AsyncImagePainter.State.Empty, is AsyncImagePainter.State.Success -> ImageStatus.Loaded
}

/**
 * A screenful of [CatItemPlaceholder]s, for any screen waiting on its first cats.
 *
 * Not scrollable: there is nothing below the skeleton to reach, and one that moves invites the
 * user to chase content that does not exist yet.
 */
@Composable
fun CatListPlaceholder(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    count: Int = PLACEHOLDER_COUNT,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        userScrollEnabled = false,
    ) {
        items(count) { CatItemPlaceholder() }
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
private val ERROR_ICON_SIZE = 40.dp

/** Two 48dp touch targets side by side, plus the breathing room around them. */
private val NOTCH_WIDTH = 104.dp
private val NOTCH_HEIGHT = 52.dp
private val NOTCH_CORNER = 20.dp
private val NOTCH_SWEEP = 16.dp

/** Two is what fits on a phone screen at [IMAGE_HEIGHT] — enough to read as a list, no more. */
private const val PLACEHOLDER_COUNT = 2

/** Declared last on purpose: top-level initialisers run in file order, and this reads the rest. */
private val CAT_CARD_SHAPE =
    CatCardShape(CARD_CORNER, NOTCH_WIDTH, NOTCH_HEIGHT, NOTCH_CORNER, NOTCH_SWEEP)

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

@Preview(name = "Placeholder", showBackground = true)
@Composable
private fun CatItemPlaceholderPreview() {
    CatsListTheme { CatItemPlaceholder() }
}

@Preview(name = "Image failed", showBackground = true)
@Composable
private fun CatImageErrorPreview() {
    CatsListTheme {
        Box(modifier = Modifier.fillMaxWidth().padding(CARD_MARGIN_HORIZONTAL)) {
            CatImageError(onRetry = {}, modifier = Modifier.fillMaxWidth().height(IMAGE_HEIGHT))
        }
    }
}
