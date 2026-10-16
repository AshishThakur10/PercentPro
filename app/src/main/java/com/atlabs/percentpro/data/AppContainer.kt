package com.atlabs.percentpro.data

import android.content.Context
import com.atlabs.percentpro.content.LessonAvailability
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.content.MockTestCatalog
import com.atlabs.percentpro.content.OnboardingRules
import com.atlabs.percentpro.content.LessonCatalog
import com.atlabs.percentpro.content.LessonContentRepository
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.model.ActivityId
import com.atlabs.percentpro.model.ActivityKind
import com.atlabs.percentpro.model.LessonId

/**
 * Manual dependency container. Phase 1 had no need for a DI framework, but
 * routing construction through one place keeps the ViewModels testable and gives
 * later phases a single seam where a quiz engine or database would be introduced.
 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext) }

    val lessonCatalog: LessonCatalog by lazy { LessonCatalog() }

    val lessonContent: LessonContentRepository by lazy { LessonContentRepository() }

    /**
     * Playable lessons are derived from what content actually exists rather than
     * from a hand-maintained list, so adding a lesson to the catalog without
     * writing its blocks cannot accidentally make it unlockable.
     */
    private val playableLessonIds: Set<LessonId> by lazy {
        LessonAvailability.implementedLessons(lessonCatalog.lessons())
            .map { it.id }
            .filter(lessonContent::isImplemented)
            .toSet()
    }

    val progressRepository: ProgressRepository by lazy {
        ProgressRepository(
            context = appContext,
            playableLessonIds = playableLessonIds,
            blockCountFor = { lessonId -> lessonContent.blocksFor(lessonId).size }
        )
    }

    val questionRepository: QuestionRepository by lazy { QuestionRepository() }

    val practiceHistoryRepository: PracticeHistoryRepository by lazy {
        PracticeHistoryRepository(appContext)
    }

    val mockTestCatalog: MockTestCatalog by lazy { MockTestCatalog() }
    val examCatalog: ExamCatalog by lazy { ExamCatalog() }

    val mockTestRepository: MockTestRepository by lazy {
        MockTestRepository(
            context = appContext,
            questionRepository = questionRepository,
            catalog = mockTestCatalog
        )
    }

    val answerLedgerRepository: AnswerLedgerRepository by lazy {
        AnswerLedgerRepository(
            store = DataStoreAnswerLedgerStore(appContext),
            marker = DataStoreAnswerLedgerMarker(appContext),
            questionRepository = questionRepository
        )
    }

    val gamificationRepository: GamificationRepository by lazy {
        GamificationRepository(
            store = DataStoreGamificationStore(appContext),
            baselineMarker = DataStoreBaselineMarker(appContext)
        )
    }

    /**
     * Recovers mock-test answers recorded before the ledger existed.
     *
     * Practice history stores counts only, so its answers cannot be recovered and
     * are deliberately not reconstructed.
     */
    suspend fun seedAnswerLedgerFromMockTests() {
        answerLedgerRepository.backfillFromMockTests(mockTestRepository.currentResults())
    }

    /**
     * Lessons and mock tests already finished before Phase 5 existed.
     *
     * Read once on first launch and used only to block a retroactive payout.
     */
    suspend fun preExistingGamificationActivity(): PreExistingActivity {
        val lessons = progressRepository.current().completedLessonIds.map { it.storageKey }.toSet()
        val mockTests = mockTestRepository.currentResults().map { it.testId.value }.toSet()
        return PreExistingActivity(lessons, mockTests)
    }

/**
 * Whether this install already has something in it.
 *
     * Delegates the decision to [OnboardingRules] so the rule is stated once and
 * * can be tested without a Context. Failures are treated as "new", because the
     * worst outcome of a wrong answer is showing a skippable tour, whereas the
     * worst outcome of the other reading would be hiding onboarding from
     * somebody who has never seen it.
     */
suspend fun hasPriorActivity(): Boolean = runCatching {
        val gamification = gamificationRepository.current()
        // Counted by kind rather than by string prefix, so a ledger id is
        // classified by the same enum the rewards use and cannot be
        // miscounted if the prefix format ever changes.
        fun ledgerCount(kind: ActivityKind): Int =
            (gamification.rewardedActivityIds + gamification.preExistingActivityIds)
                .count { ActivityId(it).kind == kind }

        OnboardingRules.shouldSkipOnboarding(
            OnboardingRules.Evidence(
                completedLessons = progressRepository.current().completedLessonIds.size,
                practiceRuns = practiceHistoryRepository.currentHistory().size,
                savedMockTests = mockTestRepository.currentResults().size,
                totalXp = gamification.totalXp,
                answeredQuestions = gamification.distinctAnsweredQuestionIds.size,
                ledgerLessons = ledgerCount(ActivityKind.LESSON_COMPLETED),
                ledgerMockTests = ledgerCount(ActivityKind.MOCK_TEST_COMPLETED)
            )
        )
    }.getOrDefault(false)
}

/** Activity recorded before gamification existed. Never awarded XP for. */
data class PreExistingActivity(
    val completedLessonKeys: Set<String>,
    val completedMockTestIds: Set<String>
)
