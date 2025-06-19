package com.melonhead.feature_settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.melonhead.feature_settings.viewmodels.SettingsViewModel
import com.melonhead.lib_app_data.models.RenderStyle
import com.melonhead.lib_core.scenes.CloseBanner
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

internal class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MangadexFollowerTheme {
                val scope = rememberCoroutineScope()

                val renderStyle by viewModel.appData.renderStyle.collectAsState()
                val dataSaver by viewModel.appData.useDataSaver.collectAsState()
                val chapterAreaTapSize by viewModel.appData.chapterTapAreaSize.collectAsState()
                val showReadChapterCount by viewModel.appData.showReadChapterCount.collectAsState()
                val autoMarkComplete by viewModel.appData.autoMarkMangaCompleted.collectAsState()
                val autoMarkReading by viewModel.appData.autoMarkMangaReading.collectAsState()

                SettingsScreen(
                    nativeRendering = renderStyle == RenderStyle.Native,
                    onNativeRenderingChanged = { scope.launch { viewModel.appData.renderStyle.setValue(if (it) RenderStyle.Native else RenderStyle.WebView) } },

                    dataSaver = dataSaver,
                    onDataSaverChanged = { scope.launch { viewModel.appData.useDataSaver.setValue(it) } },

                    chapterAreaTapSize = chapterAreaTapSize,
                    onChapterSizeChanged = { scope.launch { viewModel.appData.chapterTapAreaSize.setValue(it) } },

                    autoMarkComplete = autoMarkComplete,
                    onAutoMarkCompleteChanged = { scope.launch { viewModel.appData.autoMarkMangaCompleted.setValue(it) } },

                    autoMarkReading = autoMarkReading,
                    onAutoMarkReadingChanged = { scope.launch { viewModel.appData.autoMarkMangaReading.setValue(it) } },
                    
                    showChapterReadCount = showReadChapterCount,
                    onChapterReadCountChanged = { scope.launch { viewModel.appData.showReadChapterCount.setValue(it) } },

                    onTappedLogout = {
                        viewModel.logout()
                        finish()
                    },

                    onTappedClose = { finish() },
                )
            }
        }

    }

    companion object {
        internal fun newIntent(context: Context): Intent {
            return Intent(context, SettingsActivity::class.java)
        }
    }
}

@Composable
private fun SettingsScreen(
    nativeRendering: Boolean,
    onNativeRenderingChanged: (Boolean) -> Unit = {},

    dataSaver: Boolean,
    onDataSaverChanged: (Boolean) -> Unit = {},

    chapterAreaTapSize: Dp,
    onChapterSizeChanged: (Dp) -> Unit = {},

    autoMarkComplete: Boolean,
    onAutoMarkCompleteChanged: (Boolean) -> Unit = {},

    autoMarkReading: Boolean,
    onAutoMarkReadingChanged: (Boolean) -> Unit = {},

    showChapterReadCount: Int,
    onChapterReadCountChanged: (Int) -> Unit = {},

    onTappedClose: () -> Unit = {},
    onTappedLogout: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CloseBanner(
            title = "Settings",
            hasDescription = false,
            onDoneTapped = onTappedClose,
            onSummaryTapped = { }
        )

        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            val chapterAreaTapSizeOptions = listOf("Small" to 30.dp, "Medium" to 60.dp, "Large" to 90.dp)

            SectionHeader("Manga List")
            Option(enabled = nativeRendering, "Read Chapters Visible", "The number of already-read chapters visible for each series") {
                IntField(showChapterReadCount, enabled = it, onChapterReadCountChanged)
            }

            // todo chapter sorting?

            SectionHeader("Chapter Rendering")

            Option(enabled = true, "Use Native Rendering", "Opens chapters in a native image renderer, rather than a webview") { CheckField(nativeRendering, enabled = it, onNativeRenderingChanged) }

            Option(enabled = nativeRendering,"Use Data Saver", "Downloads chapters using the data-saver images. Only available when using native rendering.") { CheckField(dataSaver, enabled = it, onDataSaverChanged) }

            Option(enabled = nativeRendering, "Native Chapter Next/Prev Page Controls Size", "The size of the next/previous page controls in native rendering.") {
                SelectField(selectedIndex = chapterAreaTapSizeOptions.indexOfFirst { it.second == chapterAreaTapSize }, options = chapterAreaTapSizeOptions.map { it.first }, enabled = it, onSelectedIndexChanged = {
                    onChapterSizeChanged(chapterAreaTapSizeOptions[it].second)
                })
            }

            SectionHeader("Automatic Actions")

            Option(enabled = true, "Auto Mark Manga Complete", "Automatically marks manga series as completed when the last chapter is read.") { CheckField(autoMarkComplete, it, onAutoMarkCompleteChanged) }

            Option(enabled = true, "Auto Set Manga Reading Status", "Automatically marks manga series as \"Reading\" for series that were previously On Hold.") { CheckField(autoMarkReading, it, onAutoMarkReadingChanged) }

            // notifications
            // background updates

            SectionHeader("Account")

            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = onTappedLogout,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
            ) {
                Text(text = "Log Out", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ColumnScope.SectionHeader(name: String) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(name, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Box(Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))) {  }
    }
}

@Composable
private fun ColumnScope.Option(enabled: Boolean = true, name: String, description: String? = null, content: @Composable() (RowScope.(enabled: Boolean) -> Unit)) {
    Row(Modifier
        .fillMaxWidth()
        .padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f))
            if (description != null) {
                Text(text = description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f))
            }
        }
        content(enabled)
    }
}

@Composable
private fun RowScope.IntField(number: Int, enabled: Boolean, onNumberChanged: (Int) -> Unit) {
    Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        TextButton(
            enabled = enabled,
            modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 48.dp),
            contentPadding = PaddingValues(8.dp),
            onClick = {
                onNumberChanged(number - 1)
            }
        ) {
            Text("-", color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f))
        }
        Text(number.toString(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f))
        TextButton(
            enabled = enabled,
            modifier = Modifier.defaultMinSize(minWidth = 32.dp, minHeight = 48.dp),
            contentPadding = PaddingValues(8.dp),
            onClick = {
                onNumberChanged(number + 1)
            }
        ) {
            Text("+", color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f))
        }
    }
}

@Composable
private fun RowScope.CheckField(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) {
    Checkbox(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange)
}

@Composable
private fun RowScope.SelectField(selectedIndex: Int = 0, options: List<String>, enabled: Boolean = true, onSelectedIndexChanged: (Int) -> Unit) {
    Column(Modifier.wrapContentSize(), verticalArrangement =  Arrangement.Center, horizontalAlignment = Alignment.End) {
        var expanded by remember { mutableStateOf(false) }

        OutlinedButton(
            enabled = enabled,
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f)),
            onClick = { expanded = !expanded }
        ) {
            Text(options[selectedIndex], color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.5f))
        }
        DropdownMenu (
            modifier = Modifier.defaultMinSize(minWidth = 20.dp),
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            for ((index, optionText) in options.withIndex()) {
                Row {
                    DropdownMenuItem(text = {
                        Text(optionText)
                    }, onClick = {
                        onSelectedIndexChanged(index)
                        expanded = false
                    })
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenPreview() {
    MangadexFollowerTheme {
        var showChapterReadCount by remember { mutableStateOf(1) }
        var chapterAreaTapSizeIndex by remember { mutableStateOf(60.dp) }
        var nativeRendering by remember { mutableStateOf(true) }
        var dataSaver by remember { mutableStateOf(true) }
        var autoMarkComplete by remember { mutableStateOf(true) }
        var autoMarkReading by remember { mutableStateOf(true) }

        SettingsScreen(
            nativeRendering = nativeRendering,
            onNativeRenderingChanged = { nativeRendering = it },

            dataSaver = dataSaver,
            onDataSaverChanged = { dataSaver = !it },

            chapterAreaTapSize = chapterAreaTapSizeIndex,
            onChapterSizeChanged = { chapterAreaTapSizeIndex = it },

            autoMarkComplete = autoMarkComplete,
            onAutoMarkCompleteChanged = { autoMarkComplete = !it },

            autoMarkReading = autoMarkReading,
            onAutoMarkReadingChanged = { autoMarkReading = !it },

            showChapterReadCount = showChapterReadCount,
            onChapterReadCountChanged = { showChapterReadCount = it },
        )
    }
}
