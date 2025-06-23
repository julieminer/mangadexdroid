package com.melonhead.feature_settings

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.melonhead.lib_core.scenes.CloseBanner
import com.melonhead.lib_core.theme.MangadexFollowerTheme
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import com.mikepenz.aboutlibraries.ui.compose.rememberLibraries

internal class OssActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MangadexFollowerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val data by remember { derivedStateOf { buildAboutLibrariesString() } }

                    Column {
                        CloseBanner(
                            title = "Open Source Libraries",
                            hasDescription = false,
                            onDoneTapped = { finish() },
                            onSummaryTapped = { }
                        )

                        val libraries by rememberLibraries(data)
                        LibrariesContainer(libraries, modifier = Modifier.fillMaxSize())
                    }

                }
            }
        }
    }

    private fun buildAboutLibrariesString(): String {
        return resources.openRawResource(R.raw.aboutlibraries).bufferedReader().readText()
    }
}