package com.henriquesebastiao.downtify.feature.downloads

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.component.EmptyState
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme

/** Offline downloads are a later phase (see docs/roadmap.md); the tab explains that for now. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(title = {
                Text(stringResource(R.string.downloads_title), style = MaterialTheme.typography.headlineMedium)
            })
        },
    ) { padding ->
        EmptyState(
            icon = DowntifyIcons.Downloads,
            title = stringResource(R.string.downloads_coming_title),
            body = stringResource(R.string.downloads_coming_body),
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}

@PreviewLightDark
@Composable
private fun DownloadsPreview() {
    DowntifyTheme { DownloadsScreen() }
}
