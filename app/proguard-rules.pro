# PercentPro release shrinker rules.
#
# Phase 9 turned R8 on. The app needs no keep rules to stay correct: it contains
# no reflection, no serialization library, no WebView and no DI framework, so
# nothing looks a class or member up by name at runtime. Compose, DataStore,
# Lifecycle and the rest of AndroidX ship consumer rules of their own.
#
# What is kept here is not for correctness but for diagnostics: a Play Console
# crash report that says "Unknown Source" cannot be acted on, so line numbers
# and the original file name are preserved and the source file is renamed to a
# single value that does not leak the developer's local directory layout.

-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Compose keeps its own runtime rules; these two only suppress R8 noise about
# optional desktop/debug artefacts that are not shipped in an Android build.
-dontwarn org.jetbrains.annotations.**