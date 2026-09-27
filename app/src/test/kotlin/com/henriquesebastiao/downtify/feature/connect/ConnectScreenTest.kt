package com.henriquesebastiao.downtify.feature.connect

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.model.Capabilities
import com.henriquesebastiao.downtify.core.model.ServerInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectScreenTest {
    @get:Rule
    val compose = createComposeRule()

    private val info = ServerInfo("cf12", "nas.local", "Downtify", "3.2.0", 1, true, Capabilities())

    @Test
    fun connectIsDisabledUntilAnAddressIsTyped() {
        var typed = ""
        var connected = false
        compose.setContent {
            DowntifyTheme {
                ConnectScreen(
                    state = ConnectUiState(address = typed, searching = false),
                    onAddressChange = { typed = it },
                    onPickDiscovered = {},
                    onConnect = { connected = true },
                    onCodeChange = {},
                    onPair = {},
                    onBackToAddress = {},
                    onScan = {},
                )
            }
        }
        compose.onNodeWithText("Connect").assertIsNotEnabled()
        compose.onNodeWithText("Server address").performTextInput("192.168.1.20")
        assertEquals("192.168.1.20", typed)
        assertTrue(!connected)
    }

    @Test
    fun connectCallsBackWhenAnAddressIsPresent() {
        var connected = false
        compose.setContent {
            DowntifyTheme {
                ConnectScreen(
                    state = ConnectUiState(address = "http://192.168.1.20:8000", searching = false),
                    onAddressChange = {},
                    onPickDiscovered = {},
                    onConnect = { connected = true },
                    onCodeChange = {},
                    onPair = {},
                    onBackToAddress = {},
                    onScan = {},
                )
            }
        }
        compose.onNodeWithText("Connect").performScrollTo().assertIsEnabled().performClick()
        assertTrue(connected)
    }

    @Test
    fun pairStepShowsTheWrongCodeErrorAndNeedsAFullCode() {
        compose.setContent {
            DowntifyTheme {
                ConnectScreen(
                    state = ConnectUiState(
                        step = ConnectStep.Pair("http://192.168.1.20:8000", info),
                        code = "K7QM",
                        error = ConnectError.WrongCode,
                    ),
                    onAddressChange = {},
                    onPickDiscovered = {},
                    onConnect = {},
                    onCodeChange = {},
                    onPair = {},
                    onBackToAddress = {},
                    onScan = {},
                )
            }
        }
        compose.onNodeWithText("Pair").assertIsNotEnabled()
        compose.onNodeWithText("Pairing code").assertExists()
    }
}
