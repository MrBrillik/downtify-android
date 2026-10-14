package com.henriquesebastiao.downtify.ui.common

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import com.henriquesebastiao.downtify.core.designsystem.component.DowntifyIcons
import com.henriquesebastiao.downtify.core.designsystem.theme.DowntifyTheme
import com.henriquesebastiao.downtify.core.designsystem.theme.Spacing

/** What tapping a [GroupedRow] does, and what it shows at the end. */
sealed interface RowAction {
    /** Opens something; [trailingIcon] hints at what (a chevron for a screen, "open in new" for a link). */
    data class Click(val onClick: () -> Unit, @DrawableRes val trailingIcon: Int? = null) : RowAction

    /** A switch: the whole row toggles it. */
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : RowAction
}

/** One row of a [GroupedList]. */
data class GroupedRow(
    val title: String,
    val action: RowAction,
    val summary: String? = null,
    @DrawableRes val icon: Int? = null,
    val enabled: Boolean = true,
    val titleColor: Color = Color.Unspecified,
)

/**
 * A group of rows as Material 3 Expressive draws a grouped list: each row its own rounded surface,
 * the first and last with large outer corners, with a small gap between them.
 */
@Composable
fun GroupedList(rows: List<GroupedRow>, modifier: Modifier = Modifier) {
    Column(
        modifier.padding(horizontal = Spacing.screen),
        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap),
    ) {
        rows.forEachIndexed { index, row -> GroupedListItem(row, index, rows.size) }
    }
}

/** A row that is the [index]th of [count] in its group, which decides its corners. */
@Composable
fun GroupedListItem(row: GroupedRow, index: Int, count: Int, modifier: Modifier = Modifier) {
    val shapes = ListItemDefaults.segmentedShapes(index = index, count = count)
    val colors = ListItemDefaults.segmentedColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    val leading: @Composable (() -> Unit)? = row.icon?.let { icon ->
        { Icon(painterResource(icon), contentDescription = null) }
    }
    val supporting: @Composable (() -> Unit)? = row.summary?.let { summary -> { Text(summary) } }
    val title: @Composable () -> Unit = { Text(row.title, color = row.titleColor) }
    when (val action = row.action) {
        is RowAction.Click -> SegmentedListItem(
            onClick = action.onClick,
            shapes = shapes,
            modifier = modifier,
            enabled = row.enabled,
            leadingContent = leading,
            trailingContent = action.trailingIcon?.let { icon ->
                {
                    Icon(
                        painterResource(icon),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            supportingContent = supporting,
            colors = colors,
            content = title,
        )

        is RowAction.Toggle -> SegmentedListItem(
            checked = action.checked,
            onCheckedChange = action.onCheckedChange,
            shapes = shapes,
            modifier = modifier,
            enabled = row.enabled,
            leadingContent = leading,
            trailingContent = { Switch(checked = action.checked, onCheckedChange = null, enabled = row.enabled) },
            supportingContent = supporting,
            colors = colors,
            content = title,
        )
    }
}

/** A group's title, in the accent color. */
@Composable
fun GroupTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(start = Spacing.screen + Spacing.lg, end = Spacing.screen, top = Spacing.xl, bottom = Spacing.sm)
            .semantics { heading() },
    )
}

@PreviewLightDark
@Composable
private fun GroupedListPreview() {
    DowntifyTheme {
        Surface {
            Column {
                GroupTitle("Appearance")
                GroupedList(
                    listOf(
                        GroupedRow("Theme", RowAction.Click({}), "Follow system", DowntifyIcons.DarkMode),
                        GroupedRow(
                            "Pure black",
                            RowAction.Toggle(true) {
                            },
                            "True black in the dark theme",
                            DowntifyIcons.Contrast,
                        ),
                        GroupedRow("About", RowAction.Click({}, DowntifyIcons.ChevronRight), null, DowntifyIcons.Info),
                    ),
                )
            }
        }
    }
}
