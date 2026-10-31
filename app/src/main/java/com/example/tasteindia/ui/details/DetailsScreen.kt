package com.example.tasteindia.ui.details

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.tasteindia.domain.IngredientLine
import com.example.tasteindia.domain.MealDetail
import com.example.tasteindia.ui.components.AttributionFooter
import com.example.tasteindia.ui.components.MealImage
import com.example.tasteindia.ui.components.StatusMessage
import com.example.tasteindia.ui.theme.TasteIndiaTheme

@Composable
fun DetailsRoute(viewModel: DetailsViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DetailsScreen(
        state = state,
        onBack = onBack,
        onRetry = viewModel::load,
        onToggleFavourite = viewModel::toggleFavourite,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(
    state: DetailsUiState,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onToggleFavourite: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = (state as? DetailsUiState.Content)?.meal?.name ?: "Recipe",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Navigate back")
                    }
                },
                actions = {
                    if (state is DetailsUiState.Content) {
                        IconToggleButton(checked = state.isFavourite, onCheckedChange = { onToggleFavourite() }) {
                            Icon(
                                imageVector = if (state.isFavourite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                                contentDescription = "Favourite",
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when (state) {
            is DetailsUiState.Loading ->
                StatusMessage("Loading recipe…", Modifier.padding(padding), showSpinner = true)
            is DetailsUiState.Error ->
                StatusMessage(state.message, Modifier.padding(padding)) {
                    Button(onClick = onRetry) { Text("Retry") }
                }
            is DetailsUiState.Content -> MealContent(state.meal, Modifier.padding(padding))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MealContent(meal: MealDetail, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            MealImage(
                url = meal.thumbnailUrl,
                contentDescription = "Photo of the finished dish",
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)),
            )
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(meal.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
                meal.alternateName?.let {
                    Text("Also known as $it", style = MaterialTheme.typography.bodyMedium)
                }
                val meta = listOfNotNull(meal.category, meal.area).joinToString(" • ")
                if (meta.isNotEmpty()) {
                    Text(meta, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (meal.tags.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    meal.tags.forEach { tag ->
                        Surface(shape = RoundedCornerShape(8.dp), tonalElevation = 3.dp) {
                            Text("Tag: $tag", Modifier.padding(horizontal = 10.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        if (meal.ingredients.isNotEmpty()) {
            item { SectionTitle("Ingredients") }
            items(meal.ingredients) { line -> IngredientRow(line) }
        }
        if (meal.instructions.isNotEmpty()) {
            item { SectionTitle("Instructions") }
            items(meal.instructions) { paragraph ->
                Text(paragraph, style = MaterialTheme.typography.bodyLarge)
            }
        }
        val hasLinks = meal.sourceUrl != null || meal.videoUrl != null || meal.imageSourceUrl != null
        if (hasLinks) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    SectionTitle("Links")
                    meal.sourceUrl?.let { LinkButton("Open original recipe website", it, context) }
                    meal.videoUrl?.let { LinkButton("Watch recipe video", it, context) }
                    meal.imageSourceUrl?.let { LinkButton("View image credit", it, context) }
                }
            }
        }
        item { AttributionFooter() }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
}

@Composable
private fun IngredientRow(line: IngredientLine) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics(mergeDescendants = true) {},
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(line.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            line.measure?.let { Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        HorizontalDivider()
    }
}

@Composable
private fun LinkButton(label: String, url: String, context: Context) {
    OutlinedButton(onClick = { openUrl(context, url) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
}

/** URLs are validated in the mapper (http/https with a host) before they ever reach this point. */
private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "No app available to open this link", Toast.LENGTH_SHORT).show()
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailsPreview() {
    TasteIndiaTheme {
        DetailsScreen(
            state = DetailsUiState.Content(
                MealDetail(
                    id = "1", name = "Chicken Handi", alternateName = null, category = "Chicken", area = "Indian",
                    thumbnailUrl = null,
                    instructions = listOf("Heat the oil.", "Add the chicken and cook until golden."),
                    tags = listOf("Curry", "Spicy"),
                    ingredients = listOf(IngredientLine("Chicken", "1.2 kg"), IngredientLine("Salt", null)),
                    sourceUrl = "https://example.com", videoUrl = null, imageSourceUrl = null,
                ),
                isFavourite = true,
            ),
            onBack = {}, onRetry = {}, onToggleFavourite = {},
        )
    }
}
