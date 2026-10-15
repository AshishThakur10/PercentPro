package com.atlabs.percentpro.ui.screens.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.LearningActivity
import com.atlabs.percentpro.content.PracticeSession
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.data.AnswerLedgerRepository
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.PracticeHistoryRepository
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.LessonId
import com.atlabs.percentpro.model.LocalDates
import com.atlabs.percentpro.model.PracticeResult
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.PracticePlan
import com.atlabs.percentpro.model.QuestionId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PracticePhase {
    /** Answering questions one at a time. */
    ANSWERING,

    /** Questions all submitted; the summary is on screen. */
    RESULTS,

    /** The summary is on screen and the learner is scrolling the full paper. */
    REVIEW
}

data class PracticeSessionUiState(
    val topic: LessonId? = null,
    val session: PracticeSession = PracticeSession.empty(LessonId.WHAT_IS_A_PERCENTAGE),
    val phase: PracticePhase = PracticePhase.ANSWERING,
    val result: PracticeResult? = null,
    /** XP earned during this run, summed from what the ledger actually paid. */
    val xpEarnedThisSession: Int = 0,
    val unlockedAchievements: List<com.atlabs.percentpro.model.AchievementId> = emptyList()
) {
    val currentQuestion: Question? get() = session.currentQuestion
    val questionNumber: Int get() = session.questionNumber
    val questionCount: Int get() = session.questionCount
    val hasQuestions: Boolean get() = session.questionCount > 0

    /** Set once the learner reaches the last question with everything submitted. */
    val canFinish: Boolean
        get() = hasQuestions && session.unansweredCount == 0
}

/**
 * Drives one practice run.
 *
 * All scoring lives in [PracticeSession]; this class only sequences the screens and
 * persists the finished result. It never touches lesson progress, so practising a
 * topic cannot unlock or complete a lesson.
 */
class PracticeSessionViewModel(
    private val questionRepository: QuestionRepository,
    private val historyRepository: PracticeHistoryRepository,
    private val gamificationRepository: GamificationRepository,
    private val answerLedgerRepository: AnswerLedgerRepository,
    private val topic: LessonId,
    private val now: () -> Long,
    private val timeZone: () -> java.util.TimeZone = java.util.TimeZone::getDefault,
    /**
     * Optional targeted selection.
     *
     * When null the run is ordinary topic practice over every question for the
     * topic. When present the session is built from those ids only, which is how
     * a weak-topic drill or a mixed review reuses this exact engine rather than
     * introducing a second one.
     */
    private val plan: PracticePlan? = null
) : ViewModel() {

    /**
     * Identifies this run for the answer ledger and for practice history.
     *
     * A targeted run carries its plan in the id, so a normal run and a targeted
     * run of the same topic stay distinguishable. The value is stable for the
     * life of this ViewModel, and the ledger's per-question key means a repeated
     * callback inside a run still counts once.
     */
    val sessionId: String = plan?.let {
        "${it.sessionPrefix}_${it.questionIds.size}_${topic.storageKey}"
    } ?: "practice_${topic.storageKey}"

    private val _uiState = MutableStateFlow(PracticeSessionUiState())
    val uiState: StateFlow<PracticeSessionUiState> = _uiState.asStateFlow()

    init {
        val questions = plan?.questionIds
            ?.mapNotNull { questionRepository.findAcrossTopics(it) }
            ?.takeIf { it.isNotEmpty() }
            ?: questionRepository.questionsFor(topic)
        _uiState.value = PracticeSessionUiState(
            topic = topic,
            session = PracticeSession(
                topic = topic,
                questions = questions
            ),
            phase = PracticePhase.ANSWERING
        )
    }

    fun isAnswered(questionId: QuestionId): Boolean =
        _uiState.value.session.isAnswered(questionId)

    fun submittedIndexFor(questionId: QuestionId): Int? =
        _uiState.value.session.submittedIndexFor(questionId)

    /**
     * Submits an answer for the current question.
     *
     * [PracticeSession.submit] is a no-op when the question is already answered,
     * so the question id is captured before and after and a reward is only
     * reported when an answer was genuinely recorded. That is what stops a second
     * tap, or re-rendering the option list, from counting a question twice.
     */
    fun submit(optionIndex: Int) {
        val before = _uiState.value.session
        val questionId = before.currentQuestion?.id
        _uiState.update { state -> state.copy(session = state.session.submit(optionIndex)) }
        if (questionId == null) return

        val after = _uiState.value.session
        val recorded = after.submittedIndexFor(questionId) ?: return
        // Already answered before this call: nothing new to count.
        if (before.submittedIndexFor(questionId) == recorded) return

        val wasCorrect = after.isCorrect(questionId)
        viewModelScope.launch {
            // Written to the canonical answer ledger so the insights screen can
            // see this topic. Idempotent per session and question.
            answerLedgerRepository.recordAnswer(
                questionId = questionId,
                topic = after.questions.firstOrNull { it.id == questionId }?.topic ?: topic,
                source = AnswerSource.TOPIC_PRACTICE,
                sessionId = sessionId,
                wasCorrect = wasCorrect
            )
            val outcome = gamificationRepository.record(
                LearningActivity.AnsweredQuestion(
                    questionId = questionId,
                    wasCorrect = wasCorrect,
                    occurredOn = LocalDates.fromEpochMillis(now(), timeZone())
                )
            )
            if (!outcome.isEmpty) {
                _uiState.update {
                    it.copy(
                        xpEarnedThisSession = it.xpEarnedThisSession + outcome.xpAwarded,
                        unlockedAchievements = it.unlockedAchievements + outcome.unlockedAchievements
                    )
                }
            }
        }
    }

    fun next() {
        _uiState.update { state -> state.copy(session = state.session.next()) }
    }

    fun previous() {
        _uiState.update { state -> state.copy(session = state.session.previous()) }
    }

    fun goTo(index: Int) {
        _uiState.update { state -> state.copy(session = state.session.goTo(index)) }
    }

    /** True when leaving now would throw away submitted answers or an unfinished run. */
    fun shouldConfirmExit(): Boolean {
        val session = _uiState.value.session
        return session.questionCount > 0 && (session.unansweredCount > 0 || session.answeredCount > 0)
    }

    fun finish() {
        val state = _uiState.value
        if (!state.canFinish) return
        val result = state.session.toResult(now())
        _uiState.update {
            it.copy(phase = PracticePhase.RESULTS, result = result)
        }
        viewModelScope.launch {
            historyRepository.record(result)
            // Separate from lesson completion: finishing a topic practice run
            // never marks a lesson done.
            val outcome = gamificationRepository.record(
                LearningActivity.PracticeSessionCompleted(
                    topic = topic.storageKey,
                    allCorrect = result.correct == result.total && result.total > 0,
                    occurredOn = LocalDates.fromEpochMillis(now(), timeZone())
                )
            )
            if (!outcome.isEmpty) {
                _uiState.update {
                    it.copy(
                        xpEarnedThisSession = it.xpEarnedThisSession + outcome.xpAwarded,
                        unlockedAchievements = it.unlockedAchievements + outcome.unlockedAchievements
                    )
                }
            }
        }
    }

    fun startReview() {
        _uiState.update { it.copy(phase = PracticePhase.REVIEW) }
    }

    fun backToResults() {
        _uiState.update { it.copy(phase = PracticePhase.RESULTS) }
    }

    /** Starts the same topic again from the first question, clearing every answer. */
    fun restart() {
        _uiState.update { state ->
            state.copy(
                session = PracticeSession(topic, state.session.questions),
                phase = PracticePhase.ANSWERING,
                result = null,
                // A fresh run earns its rewards again from scratch.
                xpEarnedThisSession = 0,
                unlockedAchievements = emptyList()
            )
        }
    }

    companion object {
        fun factory(container: AppContainer, topic: LessonId, plan: PracticePlan? = null) = viewModelFactory {
            initializer {
                PracticeSessionViewModel(
                    questionRepository = container.questionRepository,
                    historyRepository = container.practiceHistoryRepository,
                    gamificationRepository = container.gamificationRepository,
                    answerLedgerRepository = container.answerLedgerRepository,
                    topic = topic,
                    now = System::currentTimeMillis,
                    plan = plan
                )
            }
        }
    }
}