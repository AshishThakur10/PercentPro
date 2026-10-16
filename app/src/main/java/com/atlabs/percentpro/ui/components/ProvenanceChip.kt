package com.atlabs.percentpro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R
import com.atlabs.percentpro.content.ExamCatalog
import com.atlabs.percentpro.model.AppLanguage
import com.atlabs.percentpro.model.Provenance
import com.atlabs.percentpro.model.Question

/**
 * Shows which examinations a question is written for.
 *
 * The wording is deliberately narrower than "asked in". A tag here says the
 * question was written to match that exam's section, which is the only claim the
 * app can stand behind for a question it wrote itself. A tag that did carry a
 * real year and question number would say so in its own words, because a learner
 * deciding how much to trust a label must be able to tell the two apart.
 *
 * Renders nothing when a question has no tags. Topic drills are the common case
 * and an empty row would be noise.
 */
@Composable
fun ProvenanceChips(
    question: Question,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    ProvenanceChips(provenance = question.provenance, language = language, modifier = modifier)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProvenanceChips(
    provenance: List<Provenance>,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    if (provenance.isEmpty()) return
    val catalog = remember { ExamCatalog() }

    // FlowRow, not Row: three exam chips overflow a narrow phone as soon as the
    // labels are translated, and a Row cannot wrap.
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        provenance.forEach { tag ->
            // An exam the catalog no longer defines would leave a bare id on
            // screen, so the tag is dropped rather than shown half-labelled.
            val exam = catalog.examById(tag.examId) ?: return@forEach
            val examName = stringResource(exam.titleRes)
            val paper = tag.paper.forLanguage(language)
            val section = tag.section.forLanguage(language)
            val described = stringResource(R.string.cd_exam_tag, examName, paper, section)

            // A Surface rather than a Chip: these carry no action, and a chip
            // with an empty onClick has to be faked disabled to stop looking
            // tappable, which misreports an explanation as something broken.
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.semantics { contentDescription = described }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = examName,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}