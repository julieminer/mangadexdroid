package com.melonhead.feature_settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.melonhead.feature_settings.viewmodels.SettingsViewModel
import com.melonhead.lib_core.scenes.CloseBanner
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

internal class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MangadexFollowerTheme {
                SettingsScreen(onTappedClose = { finish() })
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
    onTappedClose: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        CloseBanner(
            title = "Settings",
            hasDescription = false,
            onDoneTapped = onTappedClose,
            onSummaryTapped = { }
        )

        // manga list settings
            // chapter sorting?
            // showReadChapterCount

        // render settings
            // open in browser or webview
            // native render
                // use data saver
                // chapterTapAreaSize

        // automark as complete
        // automark as reading
        // notifications
        // background updates

        // logout
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenPreview() {
    MangadexFollowerTheme {
        SettingsScreen()
    }
}
