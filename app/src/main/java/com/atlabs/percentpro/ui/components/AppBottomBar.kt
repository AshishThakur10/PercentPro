package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.model.AppTab

@Composable
fun PercentProBottomBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp
    ) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selectedTab,
                onClick = { onTabSelected(tab) },
                icon = { TabIcon(tab) },
                label = { Text(stringResource(tab.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun TabIcon(tab: AppTab) {
    val tint = LocalContentColor.current
    when (tab) {
        AppTab.HOME -> Icon(Icons.Filled.Home, contentDescription = null, tint = tint)
        AppTab.LEARN -> Icon(Icons.AutoMirrored.Filled.List, contentDescription = null, tint = tint)
        AppTab.PRACTICE -> PracticeIcon(tint)
        AppTab.SETTINGS -> Icon(Icons.Filled.Settings, contentDescription = null, tint = tint)
    }
}

/**
 * Drawn with Compose rather than pulled from an icon font, so the app ships no
 * extra dependency for a single glyph.
 */
@Composable
private fun PracticeIcon(tint: Color) {
    Canvas(modifier = Modifier.size(24.dp)) {
        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(
            color = tint,
            radius = (size.minDimension - stroke.width) / 2f,
            style = stroke
        )
        drawCircle(color = tint, radius = size.minDimension / 4.5f, style = stroke)
        drawCircle(color = tint, radius = size.minDimension / 9f)
    }
}