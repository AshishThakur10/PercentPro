package com.atlabs.percentpro.ui.screens.mocktest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.content.MockTestCatalog
import com.atlabs.percentpro.content.MockTestCodec
import com.atlabs.percentpro.content.MockTestSelector
import com.atlabs.percentpro.content.LearningActivity
import com.atlabs.percentpro.content.MockTestSession
import com.atlabs.percentpro.content.QuestionRepository
import com.atlabs.percentpro.content.QuestionPaletteState
import com.atlabs.percentpro.data.AnswerLedgerRepository
import com.atlabs.percentpro.data.AppContainer
import com.atlabs.percentpro.data.GamificationRepository
import com.atlabs.percentpro.data.MockTestRepository
import com.atlabs.percentpro.model.AnswerSource
import com.atlabs.percentpro.model.MockTestId
import com.atlabs.percentpro.model.MockTestResult
import com.atlabs.percentpro.model.MockTestSubmission
import com.atlabs.percentpro.model.LocalDates
import com.atlabs.percentpro.model.Question
import com.atlabs.percentpro.model.RewardOutcome
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MockTestPhase {
    /** Reading the rules before the clock starts. */
    INSTRUCTIONS,

    /** The paper is live and the clock is running. */
    IN_PROGRESS,

    /** Submitted, showing the summary. */
    RESULT,

    /** Submitted, scrolling the full paper. */
    REVIEW
}

/** Why a paper cannot be started. */
sealed interface StartBlocker {

    /**
     * The bank does not yet hold enough questions for this paper.
     *
     * [available] and [required] are both reported so the instructions page can
     * say how close it is rather than only that it failed.
     */
    data class NotEnoughQuestions(val available: Int, val required: Int) : StartBlocker
}

data class MockTestExamUiState(
    val phase: MockTestPhase = MockTestPhase.INSTRUCTIONS,
    val session: MockTestSession? = null,
    val remainingMillis: Long = 0L,
    val result: MockTestResult? = null,
    val showSubmitDialog: Boolean = false,

    /**
     * Set when the reader tries to leave a paper that is still running.
     *
     * Leaving a live test throws away the attempt and the answers in it, so the
     * back gesture and the close button both stop here first. It stays false
     * outside [MockTestPhase.IN_PROGRESS]: the instructions page, a finished
     * result and the review page all have nothing to lose, and confirming an
     * exit that costs nothing is just a nuisance.
     */
    val showExitDialog: Boolean = false,
    val restoreProblem: MockTestCodec.RestoreFailure? = null,

    /**
     * Set when the paper cannot be built, so Start explains itself.
     *
     * An exam paper is drawn only from questions tagged for that exam, so it can
     * legitimately have too few. Silently doing nothing would leave the reader
     * pressing a button that never responds, which is indistinguishable from a
     * crash.
     */
    val startBlocker: StartBlocker? = null,
    val recoveredResult: MockTestResult? = null,
    /** XP and achievements from the submission that produced [result]. */
    val reward: RewardOutcome = RewardOutcome.NONE
) {
    val isLive: Boolean get() = phase == MockTestPhase.IN_PROGRESS

    val currentQuestion: Question? get() = session?.currentQuestion

    val questionNumber: Int get() = session?.questionNumber ?: 0

    val questionCount: Int get() = session?.questionCount ?: 0

    /**
     * Whether leaving right now would cost the reader their attempt.
     *
     * The single definition used by both the button handler and the dialog, so
     * the warning can never appear for a phase that has nothing to lose.
     */
    val exitWouldDiscardWork: Boolean
        get() = phase == MockTestPhase.IN_PROGRESS && session?.isSubmitted == false

    val paletteStates: List<QuestionPaletteState>
        get() = session?.paletteStates() ?: emptyList()

    fun selectedOptionFor(questionId: com.atlabs.percentpro.model.QuestionId): Int? =
        session?.selectedOptionFor(questionId)

    fun isMarkedForReview(questionId: com.atlabs.percentpro.model.QuestionId): Boolean =
        session?.isMarked(questionId) ?: false
}

/**
 * Runs one mock test.
 *
 * The clock is never a counter. The session stores an absolute deadline, and
 * [remainingMillis] is recomputed from it against system time on every tick, on
 * every lifecycle resume and after every mutation. Backgrounding the app,
 * rotating the device or restarting the process therefore cannot make the timer
 * jump, drift or reset.
 *
 * Every mutating entry point refuses to act once the paper has been submitted, so
 * a double tap on Submit or a late tap on an option cannot produce a second
 * result or change a frozen paper.
 */
class MockTestExamViewModel(
    private val mockTestCatalog: MockTestCatalog,
    private val questionRepository: QuestionRepository,
    private val examCatalog: ExamCatalog,
    private val mockTestRepository: MockTestRepository,
    private val gamificationRepository: GamificationRepository,
    private val answerLedgerRepository: AnswerLedgerRepository,
    private val testId: MockTestId,
    private val now: () -> Long,
    private val timeZone: () -> java.util.TimeZone = java.util.TimeZone::getDefault
) : ViewModel() {

    private val _uiState = MutableStateFlow(MockTestExamUiState())
    val uiState: StateFlow<MockTestExamUiState> = _uiState.asStateFlow()

    private var tickerJob: Job? = null

    init {
        viewModelScope.launch { restoreOrShowInstructions() }
    }

    // ---- lifecycle ---------------------------------------------------------

    private suspend fun restoreOrShowInstructions() {
        when (val restore = mockTestRepository.restoreActiveSession(now())) {
            is MockTestCodec.SessionRestore.Ready -> beginLiveSession(restore.session)

            is MockTestCodec.SessionRestore.Unusable -> {
                when (restore.reason) {
                    MockTestCodec.RestoreFailure.EXPIRED -> recoverExpiredSession()
                    MockTestCodec.RestoreFailure.NONE -> showInstructions()
                    else -> {
                        // Corrupt or incompatible: discard it and say so rather
                        // than restoring a paper that would score wrongly.
                        mockTestRepository.clearActiveSession()
                        _uiState.update {
                            it.copy(phase = MockTestPhase.INSTRUCTIONS, restoreProblem = restore.reason)
                        }
                    }
                }
            }

            MockTestCodec.SessionRestore.None -> showInstructions()
        }
    }

    /**
     * A paper that ran out of time while the app was closed is submitted and
     * scored rather than silently dropped, so the learner's attempt is not lost.
     */
    private suspend fun recoverExpiredSession() {
        val raw = mockTestRepository.rawActiveSession()
        // allowExpired lets the stored answers be read back so the attempt can be
        // scored rather than thrown away.
        val restored = MockTestCodec.decodeSession(
            raw = raw,
            catalog = mockTestCatalog,
            questionsById = { id -> questionRepository.findAcrossTopics(id) },
            nowEpochMillis = 0L,
            allowExpired = true
        )
        mockTestRepository.clearActiveSession()

        val session = (restored as? MockTestCodec.SessionRestore.Ready)?.session
        if (session != null) {
            submit(session, MockTestSubmission.TIMEOUT, recovered = true)
        } else {
            showInstructions()
        }
    }

    private fun showInstructions() {
        _uiState.update {
            it.copy(phase = MockTestPhase.INSTRUCTIONS, session = null, result = null)
        }
    }

    fun dismissRestoreProblem() {
        _uiState.update { it.copy(restoreProblem = null) }
    }

    // ---- starting ----------------------------------------------------------

    fun startTest() {
        val config = mockTestCatalog.testById(testId) ?: return
        viewModelScope.launch {
            val existing = mockTestRepository.restoreActiveSession(now())
            if (existing is MockTestCodec.SessionRestore.Ready) {
                beginLiveSession(existing.session)
                return@launch
            }
            mockTestRepository.clearActiveSession()

            val questions = MockTestSelector.select(
                config = config,
                topics = questionRepository.availableTopics(),
                questionsByTopic = questionRepository::questionsFor,
                difficultyMix = examCatalog.examById(config.examId ?: return@launch)
                    ?.difficultyMix
            )
            if (questions.size < config.questionCount) {
                // Say why rather than returning silently: a paper that cannot
                // fill must not leave a Start button that does nothing.
                _uiState.update {
                    it.copy(
                        startBlocker = StartBlocker.NotEnoughQuestions(
                            available = questions.size,
                            required = config.questionCount
                        )
                    )
                }
                return@launch
            }

            val startedAt = now()
            val session = MockTestSession.start(
                sessionId = newSessionId(startedAt),
                config = config,
                questions = questions,
                startedAtEpochMillis = startedAt
            )
            beginLiveSession(session)
        }
    }

    private fun newSessionId(startedAt: Long): String = "${testId.value}_$startedAt"

    private fun beginLiveSession(session: MockTestSession) {
        _uiState.update {
            it.copy(
                phase = MockTestPhase.IN_PROGRESS,
                session = session,
                remainingMillis = session.remainingMillis(now()),
                result = null,
                showSubmitDialog = false,
                startBlocker = null
            )
        }
        startTicker()
        persist()
    }

    /** Clears a "cannot start" notice so Start can be tried again. */
    fun dismissStartBlocker() {
        _uiState.update { it.copy(startBlocker = null) }
    }

    // ---- timer -------------------------------------------------------------

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = viewModelScope.launch {
            while (true) {
                tick()
                delay(TICK_MILLIS)
            }
        }
    }

    /**
     * Recomputes remaining time from the stored deadline. Called on every tick
     * and whenever the screen comes back to the foreground.
     */
    fun tick() {
        val state = _uiState.value
        val session = state.session ?: return
        if (state.phase != MockTestPhase.IN_PROGRESS) return
        if (session.isSubmitted) return

        val remaining = session.remainingMillis(now())
        if (remaining <= 0L) {
            _uiState.update { it.copy(remainingMillis = 0L) }
            submit(session, MockTestSubmission.TIMEOUT)
            return
        }
        if (remaining != state.remainingMillis) {
            _uiState.update { it.copy(remainingMillis = remaining) }
        }
    }

    // ---- answering ---------------------------------------------------------

    /**
     * Records an option for the current question.
     *
     * In a live paper, tapping an option is the submission, so this is where a
     * question enters the daily goal. The previous answer is captured first and a
     * reward is only reported when the stored answer actually changed, so
     * re-tapping the same option — or a recomposition replaying the call — cannot
     * count the question twice. A restored session is never re-reported, because
     * restoration goes through [beginLiveSession] and not through here.
     */
    fun selectOption(optionIndex: Int) {
        val question = _uiState.value.session?.currentQuestion
        val previous = question?.let { _uiState.value.session?.selectedOptionFor(it.id) }

        mutate { it.selectOption(optionIndex) }

        if (question == null) return
        val after = _uiState.value.session ?: return
        val recorded = after.selectedOptionFor(question.id) ?: return
        if (recorded == previous) return

        viewModelScope.launch {
            val wasCorrect = question.isCorrectAnswer(recorded)
            // The ledger keys on the paper's session id, which survives restore,
            // so a recovered session can never re-record the same answer.
            answerLedgerRepository.recordAnswer(
                questionId = question.id,
                topic = question.topic,
                source = AnswerSource.MOCK_TEST,
                sessionId = _uiState.value.session?.sessionId ?: return@launch,
                wasCorrect = wasCorrect
            )
            gamificationRepository.record(
                LearningActivity.AnsweredQuestion(
                    questionId = question.id,
                    wasCorrect = wasCorrect,
                    occurredOn = LocalDates.fromEpochMillis(now(), timeZone())
                )
            )
        }
    }

    fun goToQuestion(index: Int) = mutate { it.goTo(index) }

    fun nextQuestion() = mutate { it.next() }

    fun previousQuestion() = mutate { it.previous() }

    /** Clears only the current question's response. */
    fun clearResponse() = mutate { it.clearCurrentResponse() }

    fun toggleReviewMark() = mutate { it.toggleReviewForCurrent() }

    private fun mutate(block: (MockTestSession) -> MockTestSession) {
        val state = _uiState.value
        val session = state.session ?: return
        if (state.phase != MockTestPhase.IN_PROGRESS) return
        if (session.isSubmitted) return

        val updated = block(session)
        if (updated == session) return
        _uiState.update { it.copy(session = updated) }
        persist()
    }

    // ---- submitting --------------------------------------------------------

    fun requestSubmit() {
        val state = _uiState.value
        if (!state.isLive) return
        _uiState.update { it.copy(showSubmitDialog = true) }
    }

    fun dismissSubmitDialog() {
        _uiState.update { it.copy(showSubmitDialog = false) }
    }

    fun confirmSubmit() {
        val session = _uiState.value.session ?: return
        submit(session, MockTestSubmission.MANUAL)
    }

    /**
     * The single place a paper is finalised. Idempotent: once a submission is
     * recorded, later calls do nothing, which is what stops a double tap or a
     * timeout landing on top of a manual submit from producing two results.
     */
    private fun submit(
        session: MockTestSession,
        submission: MockTestSubmission,
        recovered: Boolean = false
    ) {
        if (session.isSubmitted) return
        val state = _uiState.value
        if (state.phase != MockTestPhase.IN_PROGRESS && !recovered) return

        tickerJob?.cancel()
        val completedAt = if (submission == MockTestSubmission.TIMEOUT) {
            // The clock ran out: record the finish at the deadline, not now.
            session.deadlineEpochMillis.coerceAtLeast(session.startedAtEpochMillis)
        } else {
            now().coerceAtLeast(session.startedAtEpochMillis)
        }
        val frozen = session.submit(submission)
        val result = MockTestResult.from(frozen, completedAt)

        _uiState.update {
            it.copy(
                phase = MockTestPhase.RESULT,
                session = frozen,
                remainingMillis = 0L,
                result = result,
                showSubmitDialog = false,
                recoveredResult = if (recovered) result else null
            )
        }
        viewModelScope.launch {
            mockTestRepository.recordResult(result)
            // Reported through the same guarded path as a manual submit, so a
            // timeout and a tap cannot both pay out. The ledger is keyed on the
            // test definition, so retrying the paper does not pay again.
            val outcome = gamificationRepository.record(
                LearningActivity.MockTestCompleted(
                    testId = testId.value,
                    occurredOn = LocalDates.fromEpochMillis(now(), timeZone())
                )
            )
            if (!outcome.isEmpty) {
                _uiState.update { it.copy(reward = outcome) }
            }
        }
    }

    // ---- leaving -----------------------------------------------------------

    /**
     * Asks to leave, from either the close button or the system back gesture.
     *
     * A live paper stops here and raises [MockTestExamUiState.showExitDialog]
     * instead of being dropped. Anything else leaves at once.
     */
    fun requestExit(onExit: () -> Unit) {
        if (_uiState.value.exitWouldDiscardWork) {
            _uiState.update { it.copy(showExitDialog = true) }
        } else {
            onExit()
        }
    }

    /** The reader chose to stay. The paper and the clock are untouched. */
    fun dismissExitDialog() {
        _uiState.update { it.copy(showExitDialog = false) }
    }

    /** The reader confirmed the loss. Clears the stored session, then leaves. */
    fun confirmExit(onExit: () -> Unit) {
        tickerJob?.cancel()
        _uiState.update { it.copy(showExitDialog = false) }
        viewModelScope.launch {
            // Without this the abandoned paper would be restored on the next
            // launch as though the reader had simply backgrounded the app.
            mockTestRepository.clearActiveSession()
            onExit()
        }
    }

    // ---- results and review ------------------------------------------------

    fun startReview() {
        _uiState.update { it.copy(phase = MockTestPhase.REVIEW) }
    }

    fun backToResult() {
        _uiState.update { it.copy(phase = MockTestPhase.RESULT) }
    }

    fun dismissRecoveryNotice() {
        _uiState.update { it.copy(recoveredResult = null) }
    }

    /** Starts the same test again from a clean paper. */
    fun retry() {
        tickerJob?.cancel()
        viewModelScope.launch { mockTestRepository.clearActiveSession() }
        _uiState.value = MockTestExamUiState(phase = MockTestPhase.INSTRUCTIONS)
        startTest()
    }

    private fun persist() {
        val session = _uiState.value.session ?: return
        if (session.isSubmitted) return
        viewModelScope.launch { mockTestRepository.saveActiveSession(session) }
    }

    override fun onCleared() {
        tickerJob?.cancel()
    }

    companion object {
        private const val TICK_MILLIS = 250L

        fun factory(container: AppContainer, testId: MockTestId) = viewModelFactory {
            initializer {
                MockTestExamViewModel(
                    mockTestCatalog = container.mockTestCatalog,
                    questionRepository = container.questionRepository,
                    examCatalog = container.examCatalog,
                    mockTestRepository = container.mockTestRepository,
                    gamificationRepository = container.gamificationRepository,
                    answerLedgerRepository = container.answerLedgerRepository,
                    testId = testId,
                    now = System::currentTimeMillis
                )
            }
        }
    }
}