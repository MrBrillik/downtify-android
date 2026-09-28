package com.henriquesebastiao.downtify.feature.downloads

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.downtify.core.data.offline.DownloadActivity
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.model.OfflineProgress
import com.henriquesebastiao.downtify.core.model.PlaybackContextType
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadsScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val album = DownloadItem(
        PlaybackContextType.Album,
        "alb",
        "Glass Harbor",
        null,
        OfflineProgress(total = 10, downloaded = 10, bytes = 412_000_000, overLimit = 0),
    )
    private val playlist = DownloadItem(
        PlaybackContextType.Playlist,
        "Deep Focus",
        "Deep Focus",
        null,
        OfflineProgress(total = 6, downloaded = 4, bytes = 0, overLimit = 0),
    )

    private fun state(keepLiked: Boolean = false, items: List<DownloadItem> = listOf(playlist, album)) =
        DownloadsUiState(
            loading = false,
            usedBytes = 2_400_000_000,
            limitBytes = 8_000_000_000,
            freeBytes = 5_600_000_000,
            songCount = 14,
            keepLiked = keepLiked,
            activity = DownloadActivity.WaitingForNetwork,
            items = items,
        )

    @Test
    fun showsStorageAndEachCollectionsStatus() {
        compose.setContent {
            DowntifyTheme { DownloadsScreen(state(), onKeepLiked = {}, onOpen = {}, onRemove = {}) }
        }
        compose.onNodeWithText("2.4 GB used").assertExists()
        compose.onNodeWithText("of 8.0 GB limit").assertExists()
        compose.onNodeWithText("10 songs · 412 MB").assertExists()
        compose.onNodeWithText("Downloading 4 of 6 · waiting for Wi-Fi").assertExists()
    }

    @Test
    fun togglesLikedSongsAndRemovesACollection() {
        var keep: Boolean? = null
        var removed: DownloadItem? = null
        compose.setContent {
            DowntifyTheme {
                DownloadsScreen(
                    state(keepLiked = false, items = listOf(album)),
                    onKeepLiked = { keep = it },
                    onOpen = {},
                    onRemove = { removed = it },
                )
            }
        }
        compose.onNodeWithText("Keep Liked songs offline").assertIsOff().performClick()
        assertEquals(true, keep)

        compose.onNodeWithContentDescription("Options for Glass Harbor").performClick()
        compose.onNodeWithText("Remove from this phone").performClick()
        assertEquals(album, removed)
    }

    @Test
    fun anEmptyPhoneExplainsHowToDownload() {
        compose.setContent {
            DowntifyTheme {
                DownloadsScreen(state(keepLiked = true, items = emptyList()), onKeepLiked = {
                }, onOpen = {}, onRemove = {})
            }
        }
        compose.onNodeWithText("Nothing on this phone yet").assertExists()
        compose.onNodeWithText("Keep Liked songs offline").assertIsOn()
    }
}
