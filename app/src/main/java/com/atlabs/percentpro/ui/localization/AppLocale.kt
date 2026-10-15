package com.atlabs.percentpro.ui.localization

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.text.layoutDirection
import com.atlabs.percentpro.model.AppLanguage
import java.util.Locale

/**
 * Re-resolves the whole tree against [language] by handing Compose a context
 * whose configuration carries the chosen locale.
 *
 * Switching therefore repaints immediately with no activity restart and no
 * AppCompat dependency, which matters for a learning app used in short study
 * sessions. The trade-off is that the system-level per-app language setting and
 * `Configuration` changes from the OS are not observed; if the app later needs
 * those, this can be replaced with `AppCompatDelegate.setApplicationLocales`.
 */
@Composable
fun ProvideAppLocale(language: AppLanguage, content: @Composable () -> Unit) {
    val context = LocalContext.current
    val baseConfiguration = LocalConfiguration.current
    val locale = remember(language) { Locale.forLanguageTag(language.tag) }
    val localizedContext = remember(context, baseConfiguration, locale) {
        val configuration = Configuration(baseConfiguration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(configuration)
    }

    CompositionLocalProvider(
        LocalContext provides localizedContext,
        LocalConfiguration provides localizedContext.resources.configuration,
        LocalLayoutDirection provides remember(locale) {
            if (locale.layoutDirection == android.view.View.LAYOUT_DIRECTION_RTL) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }
        }
    ) {
        content()
    }
}