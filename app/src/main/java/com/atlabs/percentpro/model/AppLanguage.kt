package com.atlabs.percentpro.model

import java.util.Locale

/**
 * Languages shipped in Phase 1. The tag is applied to the activity configuration
 * at runtime, so changing it re-resolves every string resource immediately
 * without recreating the activity.
 */
enum class AppLanguage(val tag: String) {
    ENGLISH("en"),
    HINDI("hi");

    companion object {
        fun fromTag(tag: String?): AppLanguage? = entries.firstOrNull { it.tag == tag }

        /** Falls back to Hindi on Hindi devices and English everywhere else. */
        fun forDeviceLocale(locale: Locale = Locale.getDefault()): AppLanguage =
            if (locale.language.equals(HINDI.tag, ignoreCase = true)) HINDI else ENGLISH
    }
}