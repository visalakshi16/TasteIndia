package com.example.tasteindia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.tasteindia.ui.theme.TasteIndiaTheme

/** Image with loading and failure treatment that never collapses its layout. */
@Composable
fun MealImage(url: String?, contentDescription: String?, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        ImagePlaceholder(modifier)
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
        loading = {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
        },
        error = { ImagePlaceholder(Modifier.fillMaxSize()) },
    )
}

@Composable
private fun ImagePlaceholder(modifier: Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Image unavailable",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(4.dp),
        )
    }
}

/** Shared by the Recipes and Favourites lists. Stateless and previewable. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeRow(
    name: String,
    thumbnailUrl: String?,
    subtitle: String?,
    isFavourite: Boolean,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(onClick = onClick, modifier = modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // Decorative: the title next to it already names the dish.
            MealImage(
                url = thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.size(88.dp).clip(RoundedCornerShape(12.dp)),
            )
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            // Filled vs outlined heart (shape, not just colour) + toggle semantics for screen readers.
            IconToggleButton(checked = isFavourite, onCheckedChange = { onToggleFavourite() }) {
                Icon(
                    imageVector = if (isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = "Favourite $name",
                )
            }
        }
    }
}

@Composable
fun StatusMessage(
    message: String,
    modifier: Modifier = Modifier,
    showSpinner: Boolean = false,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 32.dp, horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (showSpinner) CircularProgressIndicator()
        Text(
            text = message,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        actions()
    }
}

@Composable
fun AttributionFooter(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Recipe data and images courtesy of TheMealDB (themealdb.com).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RecipeRowPreview() {
    TasteIndiaTheme {
        RecipeRow(
            name = "Chicken Handi with a very long recipe title to test wrapping",
            thumbnailUrl = null,
            subtitle = "Chicken",
            isFavourite = true,
            onClick = {},
            onToggleFavourite = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusMessagePreview() {
    TasteIndiaTheme { StatusMessage("No recipes match your search and filters.") }
}
