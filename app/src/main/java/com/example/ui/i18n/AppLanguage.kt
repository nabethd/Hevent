package com.example.ui.i18n

import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    HEBREW("he", "עברית", true),
    ENGLISH("en", "English", false);

    companion object {
        /**
         * Hebrew has two language codes in circulation: the modern "he" and the legacy "iw".
         * Which one [Locale.getLanguage] hands back depends on the runtime — Android reports "iw",
         * recent JDKs report "he" — so match both rather than trusting either.
         */
        private val HEBREW_CODES = setOf("he", "iw")

        fun fromDeviceLocale(locale: Locale = Locale.getDefault()): AppLanguage =
            if (locale.language in HEBREW_CODES) HEBREW else ENGLISH
    }
}
