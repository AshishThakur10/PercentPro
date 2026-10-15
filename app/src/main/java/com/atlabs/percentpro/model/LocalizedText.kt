package com.atlabs.percentpro.model

/**
 * A piece of text that ships in both supported languages.
 *
 * Question banks carry their own translations rather than pointing at string
 * resources, so a question and both of its language versions always move together
 * and a future offline or bundled data source can supply them the same way. UI
 * chrome and lesson prose still live in string resources.
 */
data class LocalizedText(val english: String, val hindi: String) {

    fun forLanguage(language: AppLanguage): String = when (language) {
        AppLanguage.ENGLISH -> english
        AppLanguage.HINDI -> hindi
    }

    val isComplete: Boolean get() = english.isNotBlank() && hindi.isNotBlank()

    companion object {
        fun of(english: String, hindi: String): LocalizedText = LocalizedText(english, hindi)
    }
}