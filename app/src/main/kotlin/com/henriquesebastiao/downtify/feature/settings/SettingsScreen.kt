package com.henriquesebastiao.downtify.feature.settings

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.henriquesebastiao.downtify.BuildConfig
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.data.session.ConnectionState
import com.henriquesebastiao.downtify.core.data.settings.ThemeMode
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyLogo
import com.henriquesebastiao.downtify.core.designsystem.component.StatusPill
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing
import com.henriquesebastiao.downtify.core.model.StreamFormat
import com.henriquesebastiao.downtify.core.model.StreamQuality
import com.henriquesebastiao.downtify.core.model.Transcoding

@Composable
fun SettingsRoute(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    SettingsScreen(
        state = state,
        actions = SettingsActions(
            onBack = onBack,
            onWifiQuality = viewModel::setWifiQuality,
            onMobileQuality = viewModel::setMobileQuality,
            onTheme = viewModel::setTheme,
            onDynamicColor = viewModel::setDynamicColor,
            onUnpair = viewModel::unpair,
            onOpenWeb = {
                runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, state.baseUrl.toUri())) }
            },
        ),
        modifier = modifier,
    )
}

data class SettingsActions(
    val onBack: () -> Unit = {},
    val onWifiQuality: (StreamQuality) -> Unit = {},
    val onMobileQuality: (StreamQuality) -> Unit = {},
    val onTheme: (ThemeMode) -> Unit = {},
    val onDynamicColor: (Boolean) -> Unit = {},
    val onUnpair: () -> Unit = {},
    val onOpenWeb: () -> Unit = {},
)

private enum class SettingsDialog { WifiQuality, MobileQuality, Theme, Unpair }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(state: SettingsUiState, actions: SettingsActions, modifier: Modifier = Modifier) {
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = actions.onBack) {
                        Icon(
                            painterResource(DowntifyIcons.Back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            ServerCard(state, onChangeServer = { dialog = SettingsDialog.Unpair }, onOpenWeb = actions.onOpenWeb)

            SectionTitle(stringResource(R.string.settings_playback))
            SettingRow(
                icon = DowntifyIcons.Wifi,
                title = stringResource(R.string.settings_quality_wifi),
                summary = qualitySummary(state.settings.wifiQuality),
                onClick = { dialog = SettingsDialog.WifiQuality },
            )
            SettingRow(
                icon = DowntifyIcons.Cellular,
                title = stringResource(R.string.settings_quality_mobile),
                summary = if (state.transcoding.available) {
                    qualitySummary(state.settings.mobileQuality)
                } else {
                    stringResource(R.string.settings_quality_no_transcoding)
                },
                enabled = state.transcoding.available,
                onClick = { dialog = SettingsDialog.MobileQuality },
            )

            SectionTitle(stringResource(R.string.settings_appearance))
            SettingRow(
                icon = DowntifyIcons.DarkMode,
                title = stringResource(R.string.settings_theme),
                summary = stringResource(state.settings.theme.label),
                onClick = { dialog = SettingsDialog.Theme },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                SwitchRow(
                    icon = DowntifyIcons.Palette,
                    title = stringResource(R.string.settings_dynamic_color),
                    summary = stringResource(R.string.settings_dynamic_color_body),
                    checked = state.settings.dynamicColor,
                    onCheckedChange = actions.onDynamicColor,
                )
            }

            SectionTitle(stringResource(R.string.settings_about))
            SettingRow(
                icon = DowntifyIcons.Info,
                title = stringResource(R.string.settings_app_version, BuildConfig.VERSION_NAME),
                summary = if (state.apiVersion >
                    0
                ) {
                    stringResource(R.string.settings_server_api, state.apiVersion)
                } else {
                    null
                },
            )
            SettingRow(
                icon = DowntifyIcons.Unpair,
                title = stringResource(R.string.settings_unpair),
                summary = stringResource(R.string.settings_unpair_body),
                titleColor = MaterialTheme.colorScheme.error,
                onClick = { dialog = SettingsDialog.Unpair },
            )
        }
    }

    when (dialog) {
        SettingsDialog.WifiQuality -> ChoiceDialog(
            title = stringResource(R.string.settings_quality_wifi),
            options = state.qualityOptions,
            selected = state.settings.wifiQuality,
            label = { qualityLabel(it) },
            onSelect = actions.onWifiQuality,
            onDismiss = { dialog = null },
        )

        SettingsDialog.MobileQuality -> ChoiceDialog(
            title = stringResource(R.string.settings_quality_mobile),
            options = state.qualityOptions,
            selected = state.settings.mobileQuality,
            label = { qualityLabel(it) },
            onSelect = actions.onMobileQuality,
            onDismiss = { dialog = null },
        )

        SettingsDialog.Theme -> ChoiceDialog(
            title = stringResource(R.string.settings_theme),
            options = ThemeMode.entries,
            selected = state.settings.theme,
            label = { stringResource(it.label) },
            onSelect = actions.onTheme,
            onDismiss = { dialog = null },
        )

        SettingsDialog.Unpair -> AlertDialog(
            onDismissRequest = { dialog = null },
            icon = { Icon(painterResource(DowntifyIcons.Unpair), contentDescription = null) },
            title = { Text(stringResource(R.string.settings_unpair_title)) },
            text = { Text(stringResource(R.string.settings_unpair_message)) },
            confirmButton = {
                TextButton(onClick = {
                    dialog = null
                    actions.onUnpair()
                }) { Text(stringResource(R.string.settings_unpair_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )

        null -> Unit
    }
}

@Composable
private fun ServerCard(state: SettingsUiState, onChangeServer: () -> Unit, onOpenWeb: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = MaterialTheme.shapes.large,
        modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.sm),
    ) {
        Column(Modifier.padding(Spacing.lg), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                DowntifyLogo(size = 40.dp)
                Column(Modifier.weight(1f)) {
                    Text(state.serverName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        if (state.version.isBlank()) {
                            state.address
                        } else {
                            stringResource(
                                R.string.settings_server_subtitle,
                                state.address,
                                state.version,
                            )
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ConnectionPill(state.connection)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                OutlinedButton(onClick = onChangeServer) { Text(stringResource(R.string.settings_change_server)) }
                TextButton(onClick = onOpenWeb) { Text(stringResource(R.string.settings_open_web)) }
            }
        }
    }
}

@Composable
private fun ConnectionPill(connection: ConnectionState) {
    val scheme = MaterialTheme.colorScheme
    when (connection) {
        ConnectionState.Connected -> StatusPill(stringResource(R.string.settings_connected))

        ConnectionState.Unknown -> StatusPill(
            stringResource(R.string.settings_checking),
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
            dot = scheme.outline,
        )

        ConnectionState.Unreachable -> StatusPill(
            stringResource(R.string.settings_unreachable),
            container = scheme.surfaceContainerHighest,
            content = scheme.onSurfaceVariant,
            dot = scheme.outline,
        )

        ConnectionState.DifferentServer -> StatusPill(
            stringResource(R.string.settings_different_server),
            container = scheme.errorContainer,
            content = scheme.onErrorContainer,
            dot = scheme.error,
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(
            start = Spacing.screen,
            end = Spacing.screen,
            top = Spacing.xl,
            bottom = Spacing.xs,
        ).semantics {
            heading()
        },
    )
}

@Composable
private fun SettingRow(
    icon: Int,
    title: String,
    summary: String?,
    enabled: Boolean = true,
    titleColor: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null,
) {
    ListItem(
        headlineContent = { Text(title, color = titleColor) },
        supportingContent = summary?.let { { Text(it) } },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier,
    )
}

@Composable
private fun SwitchRow(icon: Int, title: String, summary: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Composable
private fun <T> ChoiceDialog(
    title: String,
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = option == selected, role = Role.RadioButton) {
                                onSelect(option)
                                onDismiss()
                            }
                            .padding(vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(label(option), style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}

@Composable
private fun qualitySummary(quality: StreamQuality): String = if (quality.isOriginal) {
    stringResource(R.string.settings_quality_original)
} else {
    stringResource(R.string.settings_quality_transcoded, quality.format.displayName, quality.bitrate)
}

@Composable
private fun qualityLabel(quality: StreamQuality): String = if (quality.isOriginal) {
    stringResource(R.string.settings_quality_original)
} else {
    stringResource(R.string.settings_quality_label, quality.format.displayName, quality.bitrate)
}

private val StreamFormat.displayName: String
    get() = when (this) {
        StreamFormat.Original -> "Original"
        StreamFormat.Opus -> "Opus"
        StreamFormat.Aac -> "AAC"
        StreamFormat.Mp3 -> "MP3"
    }

private val ThemeMode.label: Int
    get() = when (this) {
        ThemeMode.System -> R.string.settings_theme_system
        ThemeMode.Dark -> R.string.settings_theme_dark
        ThemeMode.Light -> R.string.settings_theme_light
    }

@PreviewLightDark
@Composable
private fun SettingsPreview() {
    DowntifyTheme {
        val transcoding = Transcoding(true, listOf("aac", "mp3", "opus"), listOf(96, 128, 160, 192, 256, 320))
        SettingsScreen(
            state = SettingsUiState(
                serverName = "nas.local",
                address = "192.168.1.20:8000",
                version = "3.2.0",
                apiVersion = 1,
                connection = ConnectionState.Connected,
                transcoding = transcoding,
                qualityOptions = SettingsViewModel.qualityOptions(transcoding),
            ),
            actions = SettingsActions(),
        )
    }
}
