package com.melonhead.feature_settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.melonhead.feature_settings.viewmodels.SettingsViewModel
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

internal class SettingsActivity : ComponentActivity() {
    private val viewModel by viewModel<SettingsViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MangadexFollowerTheme {

            }
        }

    }

    companion object {
        internal fun newIntent(context: Context): Intent {
            val intent = Intent(context, SettingsActivity::class.java)
            return intent
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScreenPreview() {
    MangadexFollowerTheme {

    }
}
