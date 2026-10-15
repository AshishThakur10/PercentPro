package com.atlabs.percentpro.ui.screens.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.atlabs.percentpro.R
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.AchievementStatus
import com.atlabs.percentpro.ui.components.SectionHeader
import com.atlabs.percentpro.ui.gamification.GamificationSnapshot
import java.text.DateFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsRoute(
    container: AppContainer,
    language: AppLanguage,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: AchievementsViewModel =
        viewModel(factory = AchievementsViewModel.factory(container))
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    AchievementsScreen(
        uiState = uiState,
        language = language,
        goalOptions = viewModel.goalOptions,
        onDailyGoalSelected = viewModel::setDailyGoal,
        onExit = onExit,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    uiState: AchievementsUiState,
    language: AppLanguage,
    goalOptions: List<Int>,
    onDailyGoalSelected: (Int) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.achievements_title),
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.mock_review_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth
            val snapshot = uiState.snapshot

            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
                LazyColumn(
                    modifier = Modifier
                        .width(contentWidth)
                        .testTag("achievements_list"),
                    contentPadding = PaddingValues(
                        start = 20.dp,
                        end = 20.dp,
                        top = 12.dp,
                        bottom = 32.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(key = "summary") {
                        SectionHeader(
                            title = stringResource(R.string.achievements_title),
                            subtitle = stringResource(
                                R.string.achievements_subtitle,
                                snapshot.unlockedAchievementCount,
                                snapshot.totalAchievementCount
                            )
                        )
                    }

                    item(key = "goal") {
                        DailyGoalCard(
                            snapshot = snapshot,
                            goalOptions = goalOptions,
                            onDailyGoalSelected = onDailyGoalSelected
                        )
                    }

                    item(key = "list_header") {
                        SectionHeader(
                            title = stringResource(R.string.gam_streak_achievements_label),
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }

                    if (snapshot.achievements.isEmpty()) {
                        item(key = "empty") {
                            Text(
                                text = stringResource(R.string.achievements_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(
                            items = snapshot.achievements,
                            key = { it.definition.id.value }
                        ) { status ->
                            AchievementRow(
                                status = status,
                                language = language,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DailyGoalCard(
    snapshot: GamificationSnapshot,
    goalOptions: List<Int>,
    onDailyGoalSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val goalDescription = stringResource(
        R.string.gam_daily_goal_progress_a11y,
        snapshot.todayQuestionCount,
        snapshot.dailyGoal
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.gam_daily_goal),
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(
                    R.string.gam_daily_goal_progress,
                    snapshot.todayQuestionCount,
                    snapshot.dailyGoal
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
            LinearProgressIndicator(
                progress = { snapshot.dailyGoalProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .height(8.dp)
                    .clearAndSetSemantics { contentDescription = goalDescription },
                color = if (snapshot.isDailyGoalReached) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.primaryContainer,
                drawStopIndicator = {}
            )
            if (snapshot.isDailyGoalReached) {
                Text(
                    text = stringResource(R.string.gam_daily_goal_reached),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = stringResource(R.string.gam_goal_picker_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            // FlowRow, not Row: three chips with a radio button and a text
            // label do not fit on one line in Hindi or at a large font scale,
            // and a Row cannot wrap or scroll.
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                goalOptions.forEach { option ->
                    FilterChip(
                        selected = snapshot.dailyGoal == option,
                        onClick = { onDailyGoalSelected(option) },
                        label = { Text(stringResource(R.string.gam_goal_option, option)) },
                        leadingIcon = {
                            RadioButton(
                                selected = snapshot.dailyGoal == option,
                                // The chip handles the click; this is the
                                // accessible state marker only.
                                onClick = null,
                                modifier = Modifier.clearAndSetSemantics { }
                            )
                        },
                        modifier = Modifier
                            // FilterChip is 32dp tall by default, below the
                            // 48dp minimum target.
                            .heightIn(min = 48.dp)
                            .selectable(
                                selected = snapshot.dailyGoal == option,
                                onClick = { onDailyGoalSelected(option) }
                            )
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    status: AchievementStatus,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val title = stringResource(status.definition.titleRes)
    val description = stringResource(status.definition.descriptionRes)
    val badge = stringResource(status.definition.badgeLabelRes)
    val stateLabel = if (status.isUnlocked) {
        title
    } else {
        stringResource(R.string.achievements_locked)
    }
    val rowDescription = stringResource(
        R.string.achievements_progress_a11y,
        title,
        stateLabel,
        status.currentValue.coerceAtMost(status.definition.target),
        status.definition.target
    )

    Card(
        modifier = modifier.semantics(mergeDescendants = true) { contentDescription = rowDescription },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = if (status.isUnlocked) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = if (status.isUnlocked) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.size(44.dp),
                    shape = CircleShape,
                    colors = CardDefaults.cardColors(
                        containerColor = if (status.isUnlocked) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {}
                Icon(
                    imageVector = if (status.isUnlocked) {
                        Icons.Filled.Star
                    } else {
                        Icons.Filled.Lock
                    },
                    contentDescription = null,
                    tint = if (status.isUnlocked) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 14.dp)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (status.isUnlocked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp)
                )

                if (status.isUnlocked) {
                    Text(
                        text = stringResource(
                            R.string.achievements_unlocked_on,
                            formatUnlockDate(status.unlockedAtEpochMillis ?: 0L, language)
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                } else if (status.progressFraction > 0f) {
                    Text(
                        text = status.progressText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    LinearProgressIndicator(
                        progress = { status.progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                            .height(6.dp)
                            .clearAndSetSemantics { },
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primaryContainer,
                        drawStopIndicator = {}
                    )
                }
            }
        }
    }
}

/** Formats an unlock timestamp in the app's own language, not the device default. */
/**
 * Cached because DateFormat.getDateInstance is not a cheap call and this runs
 * once per unlocked achievement on every recomposition of the list.
 */
private fun formatUnlockDate(epochMillis: Long, language: AppLanguage): String {
    val locale = if (language == AppLanguage.HINDI) {
        Locale.forLanguageTag("hi-IN")
    } else {
        Locale.forLanguageTag("en-IN")
    }
    return unlockDateFormatters.getOrPut(locale) {
        DateFormat.getDateInstance(DateFormat.MEDIUM, locale)
    }.format(Date(epochMillis))
}

private val unlockDateFormatters = ConcurrentHashMap<Locale, DateFormat>()
