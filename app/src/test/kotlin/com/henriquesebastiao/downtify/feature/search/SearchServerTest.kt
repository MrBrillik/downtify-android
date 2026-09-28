package com.henriquesebastiao.downtify.feature.search

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.model.CatalogSource
import com.henriquesebastiao.downtify.core.model.LinkKind
import com.henriquesebastiao.downtify.core.model.RemoteAlbum
import com.henriquesebastiao.downtify.core.model.RemoteSong
import com.henriquesebastiao.downtify.core.model.ResolvedLink
import com.henriquesebastiao.downtify.core.model.ServerJob
import com.henriquesebastiao.downtify.core.model.ServerJobStatus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h914dp")
@RunWith(AndroidJUnit4::class)
class SearchServerTest {
    @get:Rule
    val compose = createComposeRule()

    private fun song(id: String, title: String, source: CatalogSource = CatalogSource.YouTube) =
        RemoteSong(id, title, listOf("Nora Vale"), "Harbor Nights", "", 242, "https://x/$id", source, "", "2021", "{}")

    private val album =
        RemoteAlbum("alb", "Harbor Nights", "Nora Vale", "", "2021", "https://music.youtube.com/browse/alb", "Album")

    private fun show(state: SearchUiState, remote: RemoteActions = RemoteActions()) {
        compose.setContent {
            DowntifyTheme {
                SearchScreen(
                    query = state.query,
                    state = state,
                    navigation = SearchNavigation(),
                    onQueryChange = {},
                    onFilterChange = {},
                    onPlaySong = {},
                    remote = remote,
                )
            }
        }
    }

    @Test
    fun serverResultsCanBePreviewedAndDownloaded() {
        var previewed: RemoteSong? = null
        var downloaded: RemoteSong? = null
        val harbor = song("s1", "Harbor Song", CatalogSource.Spotify)
        show(
            SearchUiState(
                query = "harbor",
                server = ServerResults.Found(listOf(harbor), listOf(album)),
            ),
            RemoteActions(onPreview = { previewed = it }, onDownload = { downloaded = it }),
        )
        compose.onNodeWithText("Not in your library yet").assertExists()
        compose.onNodeWithText("Spotify").assertExists()
        compose.onNodeWithContentDescription("Play a preview of Harbor Song").performClick()
        compose.onNodeWithContentDescription("Download Harbor Song to the server").performClick()
        assertEquals(harbor, previewed)
        assertEquals(harbor, downloaded)
    }

    @Test
    fun showsWhatTheServerIsDoingWithRequestedMusic() {
        show(
            SearchUiState(
                query = "harbor",
                server = ServerResults.Found(listOf(song("s1", "Harbor Song")), listOf(album)),
                jobs = mapOf(
                    "s1" to
                        ServerJob(
                            "s1",
                            "Harbor Song",
                            "Nora Vale",
                            "Harbor Nights",
                            "",
                            ServerJobStatus.Done,
                            100f,
                            "",
                        ),
                ),
                requestedAlbums = mapOf("alb" to listOf("s1", "s2", "s3")),
            ),
        )
        compose.onNodeWithContentDescription("Downloaded to the server").assertExists()
        compose.onNodeWithText("Downloading 1/3").assertExists()
    }

    @Test
    fun aPastedLinkOffersToDownloadAllOfIt() {
        var asked: ResolvedLink? = null
        val link = ResolvedLink(
            url = "https://open.spotify.com/album/1",
            kind = LinkKind.Album,
            name = "Tides",
            subtitle = "Lumen Field",
            coverUrl = "",
            year = "2020",
            tracks = listOf(song("t1", "One"), song("t2", "Two")),
            albums = emptyList(),
        )
        show(
            SearchUiState(query = link.url, isLink = true, server = ServerResults.Link(link)),
            RemoteActions(onDownloadLink = { asked = it }),
        )
        compose.onNodeWithText("Download all 2 songs to the server").performClick()
        assertEquals(link, asked)
    }

    @Test
    fun anUnreachableServerOffersToTryAgain() {
        var retried = false
        show(
            SearchUiState(query = "harbor", server = ServerResults.Unreachable),
            RemoteActions(onRetry = { retried = true }),
        )
        compose.onNodeWithText("Try again").performClick()
        assertEquals(true, retried)
    }
}
