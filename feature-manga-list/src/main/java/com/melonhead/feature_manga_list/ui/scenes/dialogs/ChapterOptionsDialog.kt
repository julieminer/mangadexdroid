package com.melonhead.feature_manga_list.ui.scenes.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melonhead.lib_core.extensions.Previews
import com.melonhead.data_shared.models.ui.UIChapter
import com.melonhead.data_shared.models.ui.UIManga
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ChapterOptionsDialog(
    mangaChapterPair: Pair<UIManga, UIChapter>?,
    onToggleRead: (UIManga, UIChapter, Boolean) -> Unit,
    onToggleBlock: (UIChapter, Boolean) -> Unit,
    onClearCache: (UIManga, UIChapter) -> Unit,
    onDismissed: () -> Unit,
) {
    val (manga, chapter) = mangaChapterPair ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    ModalBottomSheet(sheetState = sheetState, onDismissRequest = { onDismissed() }) {
        ChapterOptionsDialogContent(
            onToggleRead = { onToggleRead(manga, chapter, it) },
            onToggleBlock = { onToggleBlock(chapter, it) },
            onClearCache = {
                onClearCache(manga, chapter)
                scope.launch { sheetState.hide() }.invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissed()
                    }
                }
            },
            mangaTitle = manga.title,
            chapterId = chapter.id,
            chapterNumber = chapter.chapter,
            isRead = chapter.read,
            isBlocked = chapter.blocked
        )
    }
}

@Composable
private fun ChapterOptionsDialogContent(
    mangaTitle: String,
    chapterId: String,
    chapterNumber: String?,
    isRead: Boolean,
    isBlocked: Boolean,
    onToggleRead: (Boolean) -> Unit,
    onToggleBlock: (Boolean) -> Unit,
    onClearCache: () -> Unit,
) {
    var isRead by remember { mutableStateOf(isRead) }
    var isBlocked by remember { mutableStateOf(isBlocked) }

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
            Text(text = mangaTitle, fontWeight = FontWeight.Medium, fontSize = 18.sp)
            if (chapterNumber != null) {
                Text(text = "Chapter $chapterNumber", fontWeight = FontWeight.Light, fontSize = 14.sp)
            }
            Text(text = "ID: $chapterId", fontWeight = FontWeight.Light, fontSize = 10.sp)
        }

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 48.dp, top = 16.dp, bottom = 8.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
            .height(2.dp)
        )

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // TODO: Add ability to rate manga
            // TODO: add ability to change reading status

            fun toggleRead() {
                isRead = isRead.not()
                onToggleRead(isRead)
            }

            fun toggleBlocked() {
                isBlocked = isBlocked.not()
                onToggleBlock(isBlocked)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { toggleRead() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Chapter Read Status")
                Switch(checked = isRead, onCheckedChange = {
                    toggleRead()
                })
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { toggleRead() }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "Block Chapter release")
                Switch(checked = isBlocked, onCheckedChange = {
                    toggleBlocked()
                })
            }

            Button(modifier = Modifier.fillMaxWidth(), onClick = onClearCache, colors = ButtonDefaults.buttonColors().copy(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onSurface,
            )) {
                Text(text = "Clear cache for Chapter")
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun ChapterOptionsDialogPreview() {
    MangadexFollowerTheme {
        val manga = Previews.previewUIManga()
        val chapter = Previews.previewUIChapters().first()
        ChapterOptionsDialog(manga to chapter, { _, _, _ -> }, { _, _ -> }, { _, _ -> }, {})
    }
}
