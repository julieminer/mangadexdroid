package com.melonhead.feature_manga_list.ui.scenes

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melonhead.data_shared.models.ui.*
import com.melonhead.lib_core.extensions.Previews
import com.melonhead.lib_core.extensions.dateOrTimeString
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import kotlinx.datetime.Instant

@Composable
internal fun ChapterListItem(
    modifier: Modifier = Modifier,
    uiChapter: UIChapter,
    uiManga: UIManga,
    connected: Boolean,
    refreshStatus: MangaRefreshStatus,
    onChapterClicked: (UIManga, UIChapter) -> Unit,
    onChapterLongPressed: (UIManga, UIChapter) -> Unit,
) {
    val context = LocalContext.current

    fun showOfflineToast() {
        Toast.makeText(context, "Offline", Toast.LENGTH_SHORT).show()
    }

    val canInteract = connected || (uiChapter.cachedPages ?: 0) > 0
    Card(modifier = modifier
        .fillMaxWidth()
        .combinedClickable(
            onClick = { if (canInteract) onChapterClicked(uiManga, uiChapter) else showOfflineToast() },
            onLongClick = { onChapterLongPressed(uiManga, uiChapter) }
        )) {
        Row(modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Chapter ${uiChapter.chapter}",
                        color = if (canInteract) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp)

                    if (uiManga.lastChapter == uiChapter.chapter) {
                        Text(
                            text = "End",
                            color = if (canInteract) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }

                    if (uiChapter.isDownloadingCache) {
                        CircularProgressIndicator(modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .size(12.dp),
                            strokeWidth = 2.dp)
                    } else if (uiChapter.cachedPages != null) {
                        Text(
                            text = "${uiChapter.cachedPages} pages",
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 10.sp,
                        )
                    }
                }
                Text(text = uiChapter.title ?: "",
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = if (canInteract) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Text(text = Instant.fromEpochSeconds(uiChapter.createdDate).dateOrTimeString(),
                    color = if (canInteract) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                    fontWeight = FontWeight.Light,
                    fontSize = 12.sp
                )
            }
            if (refreshStatus !is MangaRefreshStatus.None && uiChapter.read != true) {
                CircularProgressIndicator(modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .size(12.dp),
                    strokeWidth = 2.dp)
            } else {
                Text(modifier = Modifier.align(Alignment.CenterVertically),
                    color = if (canInteract) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    text = if (uiChapter.read != true) "NEW" else "",
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ChapterPreview() {
    MangadexFollowerTheme {
        val manga = Previews.previewUIManga()
        Column {
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first(),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = true,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(read = false),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = true,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(read = false, cachedPages = 5),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.None,
                connected = true,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(chapter = "101", cachedPages = 5),
                uiManga = manga.copy(lastChapter = "101"),
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = true,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first()
                    .copy(title = "Test Title with an extremely long title that may or may not wrap"),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = true,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
        }
    }
}


@Preview(showBackground = true)
@Composable
private fun ChapterPreviewOffline() {
    MangadexFollowerTheme {
        val manga = Previews.previewUIManga()
        Column {
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first(),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(read = false),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first(),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(read = false),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(read = false),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.None,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(read = false, cachedPages = 5),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.None,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first().copy(chapter = "101", cachedPages = 5),
                uiManga = manga.copy(lastChapter = "101"),
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
            ChapterListItem(
                uiChapter = Previews.previewUIChapters().first()
                    .copy(title = "Test Title with an extremely long title that may or may not wrap"),
                uiManga = manga,
                refreshStatus = MangaRefreshStatus.ReadStatus,
                connected = false,
                onChapterClicked = { _, _ -> },
                onChapterLongPressed = { _, _ -> })
        }
    }
}
