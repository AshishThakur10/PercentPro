# Claim evidence — store listing vs. implementation

Every claim in `store/listing-en.md` and `store/listing-hi.md` is mapped here to
the code and the tests that back it. If a listed feature is removed, the listing
must be updated; if the listing is edited, re-check this table.

Play's metadata policy rejects misleading descriptions, so this table exists to
keep the two in step.

---

## Feature claims

| Listing claim | Implemented in | Verified by |
|---|---|---|
| "15-question, 15-minute percentage mock test" | `model/MockTestConfig.kt`, `content/MockTestCatalog.kt` | `MockTestSessionTest`, `MockTestScoringTest` |
| "Mark questions for review" | `MockTestSession.toggleReviewForCurrent` | `MockTestSessionTest` |
| "question palette to jump around" | `ui/screens/mocktest/QuestionPalette.kt` | `MockTestSessionTest`, on-device only for rendering |
| "positive marking for correct answers and negative marking for incorrect" | `MockTestScoring.kt` (+1.00 / −0.25) | `MockTestScoringTest` |
| "review the whole paper afterwards" | `MockTestSavedReview` destination, `MockTestReviewScreen.kt` | `MockTestCodecTest` |
| "Every question comes with a clear explanation" | `Question.explanation`, rendered in review | `QuestionBankTest`, `QuestionBankAnswerTest` (38 tests) |
| "Questions are tagged by difficulty, so you can start easy and move up to hard" | `QuestionDifficulty` EASY/MEDIUM/HARD | `QuestionBankTest`, `CourseContentValidationTest` |
| "break your answers down by topic" | `content/LearningAnalytics.kt` | `LearningAnalyticsTest` (30 tests) |
| "compares your last twenty answers with the twenty before" | `LearningAnalytics` recent window | `LearningAnalyticsTest` |
| "points you at the topics that need the most work" | `content/RecommendationEngine.kt` | `RecommendationEngineTest` (19 tests) |
| "XP, daily goals, streaks and achievements" | `GamificationEngine`, `RewardRules` | `GamificationRewardTest`, `GamificationStreakTest`, `GamificationDailyGoalTest`, `GamificationAchievementTest` |
| "Set the daily goal yourself, to 10, 20 or 30" | `RewardRules.dailyGoalOptions` | `SettingsUiStateTest` |
| "Switch the entire app between English and Hindi at any time from Settings" | `AppTab.SETTINGS`, `SettingsScreen`, `ProvideAppLocale` | `SettingsUiStateTest`, `Phase8LocalizationTest` |
| "Lessons, questions, explanations and every screen in both languages" | `LocalizedText.forLanguage`, `values-hi/strings.xml` | `GamificationLocalizationTest`, `InsightLocalizationTest`, `Phase8LocalizationTest` — EN/HI string parity asserted |
| "The only permission is its own signature-scoped marker" | merged manifest | `PrivacyClaimTest`, verified against the built AAB |
| "no ads, no tracking and no analytics" | dependency set | `releaseRuntimeClasspath` inspected: AndroidX + Kotlin stdlib only |
| "stored in private app storage … never transmitted" | `allowBackup="false"`, DataStore in internal storage | `PrivacyClaimTest` fails the build if backup is re-enabled |
| "works with no connection" | no `INTERNET` permission | `PrivacyClaimTest`, confirmed in the built AAB manifest |
| "if you uninstall the app that data goes with it" | consequence of backup being disabled | `PrivacyClaimTest` |
| "12 lessons available" / topic names | `LessonCatalog`, `LessonContentRepository` | `CourseContentValidationTest` (29 tests), `LessonAvailabilityTest` (24 tests) |

---

## Deliberately NOT claimed

| Not claimed | Why |
|---|---|
| Past-year or real exam questions | Every question is authored for this app. `tools/question_bank_data.py` is the source; there is no external paper behind them. Claiming otherwise would be false and is exactly what the metadata policy prohibits. |
| Any official affiliation or endorsement | PercentPro is independent. The listing carries an explicit non-affiliation note. |
| Guaranteed scores, ranks or exam success | No such guarantee is possible or made. |
| Number of lessons beyond what is playable | Lessons 13–21 are catalogued but not yet written; `LessonAvailability` gates them. The listing describes the progression, not a count of 21. |
| Tablet or large-screen support | Not visually verified. Listing it would require tablet screenshots. |
| Any "exam-ready" or "guaranteed pass" phrasing | Prohibited by Play's metadata and deceptive-advertising policies. |