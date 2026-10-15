package com.atlabs.percentpro.ui.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.ui.components.LanguageSelector
import com.atlabs.percentpro.ui.components.SectionHeader

/**
 * Settings, and the fourth tab.
 *
 * This replaces the "coming soon" placeholder that used to sit here, so every
 * tab now leads somewhere real. Everything on it is either a setting the user
 * can change or a fact about their own stored data; there is deliberately no
 * "reset progress" control, and the screen says so rather than leaving the
 * absence unexplained.
 *
 * The version is read from the installed package rather than compiled in, so the
 * number shown is the number actually running.
 */
@Composable
fun SettingsRoute(
    container: AppContainer,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Resolved once per package rather than on every recomposition; PackageManager
    // lookups are comparatively expensive and the version cannot change while
    // the app is running.
    val versionName = remember(context.packageName) { readVersionName(context) }

    SettingsScreen(
        uiState = uiState,
        goalOptions = viewModel.goalOptions,
        versionName = versionName,
        onLanguageSelected = viewModel::setLanguage,
        onDailyGoalSelected = viewModel::setDailyGoal,
        onOpenAchievements = onOpenAchievements,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    goalOptions: List<Int>,
    versionName: String,
    onLanguageSelected: (AppLanguage) -> Unit,
    onDailyGoalSelected: (Int) -> Unit,
    onOpenAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Same 640dp cap as the other tab screens, so settings matches the
            // rest of the app on a tablet instead of stretching edge to edge.
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth

            Column(
                modifier = Modifier
                    .width(contentWidth)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SectionHeader(
                    title = stringResource(R.string.settings_title),
                    subtitle = stringResource(R.string.brand_tagline)
                )

                // ---- learning ----
                SettingsCard(modifier = Modifier.padding(top = 4.dp)) {
                    SectionHeader(title = stringResource(R.string.settings_section_learning))

                    LanguageSelector(
                        selected = uiState.language,
                        onLanguageSelected = onLanguageSelected,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .testTag(TEST_TAG_LANGUAGE)
                    )

                    Text(
                        text = stringResource(R.string.settings_daily_goal),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.settings_daily_goal_description),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    // FlowRow, not Row: the Hindi labels are wider and three
                    // chips side by side overflow a narrow phone in a way a
                    // single horizontal line cannot recover from.
                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 10.dp)
                            .testTag(TEST_TAG_DAILY_GOAL),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        goalOptions.forEach { goal ->
                            GoalChip(
                                goal = goal,
                                selected = goal == uiState.dailyGoal,
                                onClick = { onDailyGoalSelected(goal) }
                            )
                        }
                    }

                    SettingsValueRow(
                        label = stringResource(R.string.settings_progress),
                        value = stringResource(
                            R.string.settings_progress_value,
                            uiState.completedLessons,
                            uiState.totalLessons
                        ),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // ---- your data ----
                SettingsCard {
                    SectionHeader(title = stringResource(R.string.settings_section_data))

                    // A whole-card target rather than a small text link, so the
                    // achievement screen is reachable without hunting for it.
                    SettingsActionRow(
                        icon = { tint ->
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = null,
                                tint = tint,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        title = stringResource(R.string.settings_achievements),
                        value = stringResource(
                            R.string.settings_achievements_value,
                            uiState.unlockedAchievements,
                            uiState.totalAchievements
                        ),
                        onClick = onOpenAchievements,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .testTag(TEST_TAG_ACHIEVEMENTS)
                    )

                    Text(
                        text = stringResource(R.string.settings_privacy_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .semantics { heading() }
                    )
                    Text(
                        // Paragraph breaks survive; they stop the wall of text
                        // from reading as one run-on block.
                        text = stringResource(R.string.settings_privacy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    InfoRow(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.settings_no_reset),
                        body = stringResource(R.string.settings_no_reset_body),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                // ---- about ----
                SettingsCard {
                    SectionHeader(title = stringResource(R.string.settings_section_about))

                    SettingsValueRow(
                        label = stringResource(R.string.settings_version),
                        value = versionName,
                        modifier = Modifier
                            .padding(top = 12.dp)
                            .testTag(TEST_TAG_VERSION)
                    )

                    InfoRow(
                        icon = Icons.Filled.Build,
                        title = stringResource(
                            R.string.settings_built_by,
                            stringResource(R.string.brand_by)
                        ),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            content = content
        )
    }
}

@Composable
private fun GoalChip(
    goal: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(stringResource(R.string.gam_goal_option, goal)) },
        // FilterChip is 32dp tall by default; lift it to the 48dp target minimum.
        modifier = Modifier.heightIn(min = 48.dp),
        shape = MaterialTheme.shapes.small,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = MaterialTheme.colorScheme.outline
        )
    )
}

/** A label and its current value, used for the read-only facts. */
@Composable
private fun SettingsValueRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            // weight lets the value wrap onto its own line in Hindi or at a
            // large font scale instead of being clipped by a fixed row height.
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: @Composable (Color) -> Unit,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick)
            .semantics { role = Role.Button }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon(MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    title: String,
    body: String? = null,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier.fillMaxWidth()) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (body != null) {
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * The version of the running package.
 *
 * Read from the installed package metadata, which is the value the user would
 * find in their system settings. Falls back to a dash rather than an empty gap
 * if the lookup ever fails, so the row is never silently blank.
 */
private fun readVersionName(context: Context): String {
    val info = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    } catch (e: PackageManager.NameNotFoundException) {
        null
    }
    return info?.versionName?.takeIf { it.isNotBlank() } ?: "—"
}

const val TEST_TAG_LANGUAGE = "settings_language"
const val TEST_TAG_DAILY_GOAL = "settings_daily_goal"
const val TEST_TAG_ACHIEVEMENTS = "settings_achievements"
const val TEST_TAG_VERSION = "settings_version"