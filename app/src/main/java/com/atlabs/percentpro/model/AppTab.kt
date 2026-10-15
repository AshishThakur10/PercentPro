package com.atlabs.percentpro.model

import androidx.annotation.StringRes
import com.atlabs.percentpro.R

enum class AppTab(@StringRes val labelRes: Int) {
    HOME(R.string.tab_home),
    LEARN(R.string.tab_learn),
    PRACTICE(R.string.tab_practice),

    /**
     * Settings, not a profile.
     *
     * Phase 8 turned the placeholder "your profile is on the way" screen into
     * a real settings destination, so the fourth tab now leads somewhere that
     * works instead of somewhere that apologises for not existing.
     */
    SETTINGS(R.string.tab_settings)
}