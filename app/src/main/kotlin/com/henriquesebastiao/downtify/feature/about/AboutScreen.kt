package com.henriquesebastiao.downtify.feature.about

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.henriquesebastiao.downtify.BuildConfig
import com.henriquesebastiao.downtify.R
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyLogo
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing
import com.henriquesebastiao.downtify.ui.common.GroupedList
import com.henriquesebastiao.downtify.ui.common.GroupedRow
import com.henriquesebastiao.downtify.ui.common.RowAction

private const val ISSUES_URL = "https://github.com/henriquesebastiao/downtify-android/issues"
private const val SOURCE_URL = "https://github.com/henriquesebastiao/downtify-android"
private const val BUY_ME_A_COFFEE_URL = "https://www.buymeacoffee.com/henriquesebastiao"
private const val GITHUB_SPONSORS_URL = "https://github.com/sponsors/henriquesebastiao"

@Composable
fun AboutRoute(onBack: () -> Unit, onOpenLicense: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val versionLabel = stringResource(R.string.about_version_value, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    val copiedMessage = stringResource(R.string.about_version_copied)
    AboutScreen(
        versionLabel = versionLabel,
        actions = AboutActions(
            onBack = onBack,
            onReportBug = { openLink(context, ISSUES_URL) },
            onCopyVersion = { copyToClipboard(context, versionLabel, copiedMessage) },
            onOpenSource = { openLink(context, SOURCE_URL) },
            onOpenLicense = onOpenLicense,
            onOpenBuyMeACoffee = { openLink(context, BUY_ME_A_COFFEE_URL) },
            onOpenGithubSponsors = { openLink(context, GITHUB_SPONSORS_URL) },
        ),
        modifier = modifier,
    )
}

data class AboutActions(
    val onBack: () -> Unit = {},
    val onReportBug: () -> Unit = {},
    val onCopyVersion: () -> Unit = {},
    val onOpenSource: () -> Unit = {},
    val onOpenLicense: () -> Unit = {},
    val onOpenBuyMeACoffee: () -> Unit = {},
    val onOpenGithubSponsors: () -> Unit = {},
)

private enum class AboutSheet { Terms, Donate }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(versionLabel: String, actions: AboutActions, modifier: Modifier = Modifier) {
    var sheet by rememberSaveable { mutableStateOf<AboutSheet?>(null) }
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.about_title)) },
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
            AboutHeader(versionLabel)

            GroupedList(
                listOf(
                    GroupedRow(
                        title = stringResource(R.string.about_report_bug),
                        summary = stringResource(R.string.about_report_bug_body),
                        icon = DowntifyIcons.BugReport,
                        action = RowAction.Click(actions.onReportBug, DowntifyIcons.OpenInNew),
                    ),
                    GroupedRow(
                        title = stringResource(R.string.about_version),
                        summary = versionLabel,
                        icon = DowntifyIcons.Info,
                        action = RowAction.Click(actions.onCopyVersion, DowntifyIcons.Copy),
                    ),
                    GroupedRow(
                        title = stringResource(R.string.about_source),
                        summary = stringResource(R.string.about_source_body),
                        icon = DowntifyIcons.Code,
                        action = RowAction.Click(actions.onOpenSource, DowntifyIcons.OpenInNew),
                    ),
                ),
            )

            Spacer(Modifier.height(Spacing.lg))
            GroupedList(
                listOf(
                    GroupedRow(
                        title = stringResource(R.string.about_license),
                        summary = stringResource(R.string.license_name),
                        icon = DowntifyIcons.License,
                        action = RowAction.Click(actions.onOpenLicense, DowntifyIcons.ChevronRight),
                    ),
                    GroupedRow(
                        title = stringResource(R.string.about_terms),
                        summary = stringResource(R.string.about_terms_body),
                        icon = DowntifyIcons.Terms,
                        action = RowAction.Click({ sheet = AboutSheet.Terms }),
                    ),
                    GroupedRow(
                        title = stringResource(R.string.about_donate),
                        summary = stringResource(R.string.about_donate_body),
                        icon = DowntifyIcons.Donate,
                        action = RowAction.Click({ sheet = AboutSheet.Donate }),
                    ),
                ),
            )
            Spacer(Modifier.height(Spacing.xl))
        }
    }

    when (sheet) {
        AboutSheet.Terms -> TermsSheet(onDismiss = { sheet = null })

        AboutSheet.Donate -> DonateSheet(
            onBuyMeACoffee = actions.onOpenBuyMeACoffee,
            onGithubSponsors = actions.onOpenGithubSponsors,
            onDismiss = { sheet = null },
        )

        null -> Unit
    }
}

@Composable
private fun AboutHeader(versionLabel: String) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = Spacing.screen, vertical = Spacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        DowntifyLogo(size = 72.dp)
        Text(
            stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            versionLabel,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            stringResource(R.string.about_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TermsSheet(onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                stringResource(R.string.terms_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            TermsSection(R.string.terms_use_title, R.string.terms_use_body)
            TermsSection(R.string.terms_responsibility_title, R.string.terms_responsibility_body)
            TermsSection(R.string.terms_affiliation_title, R.string.terms_affiliation_body)
            TermsSection(R.string.terms_warranty_title, R.string.terms_warranty_body)
            TermsSection(R.string.terms_privacy_title, R.string.terms_privacy_body)
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun TermsSection(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        Text(
            stringResource(body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DonateSheet(onBuyMeACoffee: () -> Unit, onGithubSponsors: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.screen)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        ) {
            Text(
                stringResource(R.string.donate_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                stringResource(R.string.donate_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            DonateOption(
                icon = DowntifyIcons.Coffee,
                title = R.string.donate_buy_me_a_coffee,
                body = R.string.donate_buy_me_a_coffee_body,
                onClick = onBuyMeACoffee,
            )
            DonateOption(
                icon = DowntifyIcons.Favorite,
                title = R.string.donate_github_sponsors,
                body = R.string.donate_github_sponsors_body,
                onClick = onGithubSponsors,
            )
            Spacer(Modifier.height(Spacing.lg))
        }
    }
}

@Composable
private fun DonateOption(icon: Int, title: Int, body: Int, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Icon(painterResource(icon), contentDescription = null)
        Spacer(Modifier.padding(horizontal = Spacing.sm))
        Column(Modifier.weight(1f)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Icon(painterResource(DowntifyIcons.OpenInNew), contentDescription = null)
    }
}

private fun openLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

private fun copyToClipboard(context: Context, text: String, confirmation: String) {
    val clipboard = context.getSystemService(android.content.ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText("version", text))
    // Android 13+ shows its own confirmation.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, confirmation, Toast.LENGTH_SHORT).show()
    }
}

@PreviewLightDark
@Composable
private fun AboutPreview() {
    DowntifyTheme {
        AboutScreen(versionLabel = "1.4.0 (14)", actions = AboutActions())
    }
}
