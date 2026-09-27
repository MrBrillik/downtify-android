package com.henriquesebastiao.downtify.feature.player

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.player.PlayerState
import com.henriquesebastiao.downtify.ui.common.PreviewData
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MiniPlayerTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsTheTrackAndTogglesPlayback() {
        val track = PreviewData.tracks[1]
        var toggles = 0
        var opened = 0
        compose.setContent {
            DowntifyTheme {
                MiniPlayer(
                    state = PlayerState(track = track, isPlaying = true, positionMs = 1_000, durationMs = 200_000),
                    onOpen = { opened++ },
                    onTogglePlay = { toggles++ },
                )
            }
        }
        compose.onNodeWithText(track.displayTitle).assertExists().performClick()
        compose.onNodeWithContentDescription("Pause").performClick()
        assertEquals(1, opened)
        assertEquals(1, toggles)
    }

    @Test
    fun rendersNothingWithoutATrack() {
        compose.setContent {
            DowntifyTheme { MiniPlayer(state = PlayerState(), onOpen = {}, onTogglePlay = {}) }
        }
        compose.onNodeWithContentDescription("Play").assertDoesNotExist()
    }
}
