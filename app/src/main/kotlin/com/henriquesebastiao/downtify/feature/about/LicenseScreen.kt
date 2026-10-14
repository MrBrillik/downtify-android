package com.henriquesebastiao.downtify.feature.about

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing
import com.henriquesebastiao.downtify.core.model.LicensePart
import com.henriquesebastiao.downtify.core.model.LicenseText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private class LicenseItem(@StringRes val title: Int, @StringRes val body: Int)

private val permissions = listOf(
    LicenseItem(R.string.license_permission_commercial, R.string.license_permission_commercial_body),
    LicenseItem(R.string.license_permission_modification, R.string.license_permission_modification_body),
    LicenseItem(R.string.license_permission_distribution, R.string.license_permission_distribution_body),
    LicenseItem(R.string.license_permission_patent, R.string.license_permission_patent_body),
    LicenseItem(R.string.license_permission_private, R.string.license_permission_private_body),
)
private val conditions = listOf(
    LicenseItem(R.string.license_condition_notice, R.string.license_condition_notice_body),
    LicenseItem(R.string.license_condition_changes, R.string.license_condition_changes_body),
    LicenseItem(R.string.license_condition_source, R.string.license_condition_source_body),
    LicenseItem(R.string.license_condition_same, R.string.license_condition_same_body),
)
private val limitations = listOf(
    LicenseItem(R.string.license_limitation_liability, R.string.license_limitation_liability_body),
    LicenseItem(R.string.license_limitation_warranty, R.string.license_limitation_warranty_body),
)

/** The text in `res/raw/license.txt` split into headings and paragraphs, or why there is none yet. */
private sealed interface LicenseLoad {
    data object Loading : LicenseLoad

    data object Failed : LicenseLoad

    data class Loaded(val parts: List<LicensePart>) : LicenseLoad
}

@Composable
fun LicenseRoute(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val load by produceState<LicenseLoad>(LicenseLoad.Loading) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val raw = context.resources.openRawResource(R.raw.license).bufferedReader().use { it.readText() }
                LicenseLoad.Loaded(LicenseText.parts(raw))
            }.getOrDefault(LicenseLoad.Failed)
        }
    }
    LicenseScreen(
        fullText = (load as? LicenseLoad.Loaded)?.parts,
        failed = load == LicenseLoad.Failed,
        onBack = onBack,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LicenseScreen(fullText: List<LicensePart>?, failed: Boolean, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.license_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(DowntifyIcons.Back),
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                horizontal = Spacing.screen,
                vertical = Spacing.lg,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    Text(
                        stringResource(R.string.license_name),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        stringResource(R.string.license_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { LicenseGroup(R.string.license_permissions, permissions) }
            item { LicenseGroup(R.string.license_conditions, conditions) }
            item { LicenseGroup(R.string.license_limitations, limitations) }
            item {
                Text(
                    stringResource(R.string.license_full_text),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = Spacing.lg).semantics { heading() },
                )
            }
            if (failed) {
                item {
                    Text(
                        stringResource(R.string.license_text_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (fullText != null) {
                items(fullText) { part -> LicensePartText(part) }
            }
        }
    }
}

@Composable
private fun LicenseGroup(@StringRes title: Int, items: List<LicenseItem>) {
    Column(
        Modifier.padding(top = Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(
            stringResource(title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.semantics { heading() },
        )
        items.forEach { item ->
            Column {
                Text(stringResource(item.title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(item.body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LicensePartText(part: LicensePart) {
    when (part) {
        is LicensePart.Heading -> Text(
            part.text,
            style = MaterialTheme.typography.titleSmall,
            textAlign = if (part.text.contains('\n')) TextAlign.Center else TextAlign.Start,
            modifier = Modifier.padding(top = Spacing.md).semantics { heading() },
        )

        is LicensePart.Paragraph -> Text(
            part.text,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Default),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewLightDark
@Composable
private fun LicensePreview() {
    DowntifyTheme {
        LicenseScreen(
            fullText = listOf(
                LicensePart.Heading("GNU GENERAL PUBLIC LICENSE\nVersion 3, 29 June 2007"),
                LicensePart.Heading("0. Definitions."),
                LicensePart.Paragraph("\"This License\" refers to version 3 of the GNU General Public License."),
            ),
            failed = false,
            onBack = {},
        )
    }
}
