package com.melonhead.lib_core.scenes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.melonhead.data_shared.models.ui.UIManga
import com.melonhead.lib_core.extensions.Previews
import com.melonhead.lib_core.theme.MangadexFollowerTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MangaSummaryDialog(
    manga: UIManga?,
    onDismissed: () -> Unit,
) {
    if (manga != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        ModalBottomSheet(sheetState = sheetState, onDismissRequest = { onDismissed() }) {
            MangaOptionsDialogContent(
                mangaTitle = manga.title,
                mangaDesc = manga.description
            )
        }
    }
}

@Composable
private fun MangaOptionsDialogContent(
    mangaTitle: String,
    mangaDesc: String?,
) {
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
            if (mangaDesc != null) {
                Text(text = mangaDesc, fontWeight = FontWeight.Light, fontSize = 14.sp)
            }
        }
    }
}

@Composable
@Preview(showBackground = true, showSystemUi = true)
private fun MangaOptionsDialogPreview() {
    MangadexFollowerTheme {
        MangaSummaryDialog(Previews.previewUIManga(), {})
    }
}
