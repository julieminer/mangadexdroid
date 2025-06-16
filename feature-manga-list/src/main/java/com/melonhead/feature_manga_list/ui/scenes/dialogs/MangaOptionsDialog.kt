package com.melonhead.feature_manga_list.ui.scenes.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melonhead.data_shared.models.ui.UIManga
import com.melonhead.lib_core.extensions.Previews
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MangaOptionsDialog(
    manga: UIManga?,
    onChangeTitle: (UIManga, String) -> Unit,
    onToggleRendering: (UIManga, Boolean) -> Unit,
    onClearCache: (UIManga) -> Unit,
    onViewWebTapped: (UIManga) -> Unit,
    onChangeRatingTapped: (UIManga) -> Unit,
    onDismissed: () -> Unit,
) {
    if (manga != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val scope = rememberCoroutineScope()

        var showTitleChangeDialogForManga by remember { mutableStateOf(false) }
        if (showTitleChangeDialogForManga) {
            TitleChangeDialog(
                manga,
                onChangeMangaTitle = { uiManga, title -> onChangeTitle(uiManga, title) },
                onDismissed = { showTitleChangeDialogForManga = false }
            )
        }

        ModalBottomSheet(sheetState = sheetState, onDismissRequest = { onDismissed() }) {
            MangaOptionsDialogContent(
                mangaTitle = manga.title,
                mangaDesc = manga.description,
                usesWebView = manga.useWebview,
                mangaRating = manga.rating,
                onChangeTitle = { showTitleChangeDialogForManga = true },
                onToggleRendering = { onToggleRendering(manga, it) },
                onViewWebTapped = { onViewWebTapped(manga) },
                onChangeRatingTapped = { onChangeRatingTapped(manga) },
                onClearCache = {
                    onClearCache(manga)
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        if (!sheetState.isVisible) {
                            onDismissed()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun MangaOptionsDialogContent(
    mangaTitle: String,
    mangaDesc: String?,
    mangaRating: Int?,
    usesWebView: Boolean,
    onChangeTitle: () -> Unit,
    onToggleRendering: (Boolean) -> Unit,
    onViewWebTapped: () -> Unit,
    onChangeRatingTapped: () -> Unit,
    onClearCache: () -> Unit,
) {
    var usesWebView by remember { mutableStateOf(usesWebView) }

    fun toggleWebView() {
        usesWebView = usesWebView.not()
        onToggleRendering(usesWebView)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = mangaTitle, fontWeight = FontWeight.Medium, fontSize = 18.sp)

                Row(
                    Modifier.clickable { onChangeRatingTapped() }.padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (mangaRating == null) {
                        Icon(Icons.Outlined.Star, contentDescription = null)
                    } else {
                        Text("$mangaRating", fontWeight = FontWeight.Medium, fontSize = 18.sp)
                        Icon(Icons.Filled.Star, contentDescription = null)
                    }
                }
            }
            if (mangaDesc != null) {
                Text(text = mangaDesc, fontWeight = FontWeight.Light, fontSize = 14.sp)
            }
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 8.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            .height(2.dp)
        )

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // TODO: add ability to change reading status

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { toggleWebView() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Render Chapters with WebView")
                Switch(checked = usesWebView, onCheckedChange = {
                    toggleWebView()
                })
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = onViewWebTapped) {
                Text(text = "View Series On Web")
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = onChangeTitle) {
                Text(text = "Change Manga Title")
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = onClearCache, colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )) {
                Text(text = "Clear cache for Manga")
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun MangaOptionsDialogPreview() {
    MangadexFollowerTheme {
        MangaOptionsDialog(Previews.previewUIManga(), { _, _ -> }, { _, _ -> }, {}, {}, {}, {})
    }
}
