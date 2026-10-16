package com.atlabs.percentpro.ui.screens.practice

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.content.formatMarks
import com.atlabs.percentpro.content.formatPenalty
import com.atlabs.percentpro.model.ExamId
import com.atlabs.percentpro.model.ExamYearPaper
import com.atlabs.percentpro.model.ExamYearScheme
import com.atlabs.percentpro.model.MockTestId

/** One row on the examination list. */
internal data class ExamPapersEntry(
    val examId: ExamId,
    @StringRes val titleRes: Int,
    val yearCount: Int,
    /** Years the app has no confirmed scheme for, so the reader can see the gap. */
    val gapYears: Int
)

internal fun ExamCatalog.papersEntries(): List<ExamPapersEntry> = available().map { exam ->
    val last = lastYear(exam.id) ?: 0
    ExamPapersEntry(
        examId = exam.id,
        titleRes = exam.titleRes,
        yearCount = papersFor(exam.id, includeEveryYear = true).size,
        gapYears = unverifiedRanges(exam.id).sumOf { scheme ->
            (minOf(scheme.lastYear, last) - scheme.fromYear + 1).coerceAtLeast(0)
        }
    )
}

/**
 * Which examinations have year-by-year papers.
 *
 * A screen of its own rather than a fold on the practice tab because the archive
 * is browsed deliberately: someone looking for the 2019 pattern is not scrolling
 * a list of twenty topics.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPapersRoute(
    onExamSelected: (ExamId) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exams = remember { ExamCatalog() }
    ExamPapersList(
        entries = exams.papersEntries(),
        onExamSelected = onExamSelected,
        onExit = onExit,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamPapersList(
    entries: List<ExamPapersEntry>,
    onExamSelected: (ExamId) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { BackTopBar(title = stringResource(R.string.practice_papers_by_year_title), onExit = onExit) }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .width(contentWidth)
                .testTag("exam_papers_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "subtitle") {
                Text(
                    text = stringResource(R.string.practice_papers_by_year_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            item(key = "scope") {
                Text(
                    text = stringResource(R.string.exam_papers_scope_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.exam_papers_no_exams),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            items(entries, key = { it.examId.value }) { entry ->
                ExamPapersEntryCard(entry = entry, onClick = { onExamSelected(entry.examId) })
            }
        }
            }
        }
    }
}

/**
 * The years one examination has papers for.
 *
 * Opens on scheme eras rather than single years. An exam that has not changed its
 * marking in seven years has one thing to practise, not seven, and listing those
 * years separately would offer the same paper over and over while implying the
 * archive was deeper than it is. Every year is one tap away for a reader who
 * wants to see the record itself.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExamPaperYearsRoute(
    examId: ExamId,
    onPaperSelected: (MockTestId) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    val exams = remember { ExamCatalog() }
    val exam = exams.examById(examId)
    val gaps = exams.unverifiedRanges(examId)

    // Survives recomposition rather than being carried in the destination key:
    // the key names the exam, and the view of it is this screen's own business.
    var everyYear by rememberSaveable { mutableStateOf(false) }
    val papers = exams.papersFor(examId, includeEveryYear = everyYear)

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            BackTopBar(
                title = exam?.let { stringResource(it.titleRes) }
                    ?: stringResource(R.string.practice_papers_by_year_title),
                onExit = onExit
            )
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        LazyColumn(
            modifier = Modifier
                .width(contentWidth)
                .testTag("exam_paper_years_list"),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "scope") {
                Text(
                    text = stringResource(R.string.exam_papers_scope_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (exams.schemesFor(examId).size > 1 || gaps.isNotEmpty()) {
                item(key = "view_toggle") {
                    Column {
                        Text(
                            text = stringResource(
                                if (everyYear) {
                                    R.string.exam_papers_years_label
                                } else {
                                    R.string.exam_papers_eras_label
                                }
                            ),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = { everyYear = !everyYear },
                            modifier = Modifier.heightIn(min = 48.dp)
                        ) {
                            Text(
                                stringResource(
                                    if (everyYear) {
                                        R.string.exam_papers_show_eras
                                    } else {
                                        R.string.exam_papers_show_years
                                    }
                                )
                            )
                        }
                    }
                }
            }

            // A gap is shown above the papers, not hidden below: a reader
            // deciding whether an archive is trustworthy needs to know what is
            // missing before reading what is present.
            items(gaps, key = { "gap_${it.fromYear}" }) { scheme ->
                SchemeGapCard(scheme = scheme)
            }

            items(papers, key = { it.id }) { paper ->
                YearPaperCard(
                    paper = paper,
                    onClick = { onPaperSelected(MockTestId(paper.id)) }
                )
            }
        }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BackTopBar(title: String, onExit: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1
            )
        },
        navigationIcon = {
            IconButton(onClick = onExit) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.exam_papers_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun ExamPapersEntryCard(
    entry: ExamPapersEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(entry.titleRes)
    val yearText = pluralStringResource(
        R.plurals.exam_papers_years_available,
        entry.yearCount,
        entry.yearCount
    )
    val gapText = if (entry.gapYears > 0) {
        pluralStringResource(R.plurals.exam_papers_years_missing, entry.gapYears, entry.gapYears)
    } else {
        stringResource(R.string.exam_papers_no_gap)
    }
    // Resolved before the semantics block: that lambda is not a composable scope,
    // so it cannot resolve a string resource.
    val described = stringResource(R.string.cd_exam_papers_card, title, yearText, gapText)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = described },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = yearText,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = gapText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * One paper: an era by default, a single year when the reader asks for them.
 *
 * The scheme line carries the figures a reader is choosing between, so two eras
 * of one exam are distinguishable without opening either.
 */
@Composable
private fun YearPaperCard(
    paper: ExamYearPaper,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = paper.scheme
    val label = paper.year?.toString() ?: scheme.yearRangeLabel()
    val schemeLine = pluralStringResource(
        R.plurals.exam_papers_scheme_line,
        scheme.sectionQuestionCount,
        scheme.sectionQuestionCount,
        scheme.marksPerCorrect.formatMarks(),
        scheme.negativeMark.formatPenalty()
    )
    val questionLine = pluralStringResource(
        R.plurals.exam_papers_paper_questions,
        paper.questionCount,
        paper.questionCount
    )
    val timerLine = pluralStringResource(
        R.plurals.exam_papers_paper_minutes,
        if (scheme.hasSectionalTimer) scheme.sectionDurationMinutes else scheme.paperDurationMinutes,
        if (scheme.hasSectionalTimer) scheme.sectionDurationMinutes else scheme.paperDurationMinutes
    )
    val described = stringResource(
        R.string.cd_exam_papers_card,
        label,
        questionLine,
        "$schemeLine. $timerLine."
    )

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = described },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = questionLine,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = schemeLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
            Text(
                text = timerLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** An era the app could not confirm, shown rather than quietly dropped. */
@Composable
private fun SchemeGapCard(
    scheme: ExamYearScheme,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = stringResource(R.string.exam_papers_gap_title),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = stringResource(
                    R.string.exam_papers_gap_reason,
                    scheme.yearRangeLabel()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
    }
}
