package com.atlabs.percentpro.ui.screens.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.atlabs.percentpro.R

/**
 * The three welcome pages shown once, on the very first launch.
 *
 * Every route out of this screen — finishing the last page or pressing Skip —
 * reports through the same [onFinished], because the caller writes one flag for
 * both. That is what stops a skipped tour from reappearing next launch.
 *
 * Content lives in a scrollable column rather than a fixed stack, so the pages
 * stay readable at the largest font scale instead of clipping the body copy.
 */
private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val icon: ImageVector
)

private val PAGES = listOf(
    OnboardingPage(
        titleRes = R.string.welcome_title,
        bodyRes = R.string.onboarding_body_1,
        icon = Icons.Filled.List
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_title_2,
        bodyRes = R.string.onboarding_body_2,
        icon = Icons.Filled.Edit
    ),
    OnboardingPage(
        titleRes = R.string.onboarding_title_3,
        bodyRes = R.string.onboarding_body_3,
        icon = Icons.Filled.Lock
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    // rememberSaveable, not remember: rotating the device mid-tour must not send
    // the reader back to page one.
    var page by rememberSaveable { mutableIntStateOf(0) }
    val isLastPage = page == PAGES.lastIndex

    Scaffold(
        modifier = modifier.testTag(TEST_TAG_ONBOARDING),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Matches the 640dp cap used by the rest of the app, so the tour is
            // the same width on a tablet as it is on a phone.
            val contentWidth = if (maxWidth > 640.dp) 640.dp else maxWidth

            Column(
                modifier = Modifier
                    .width(contentWidth)
                    .align(Alignment.TopCenter)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = PAGES.size,
                    currentPage = page
                )

                PAGES[page].let { onboardingPage ->
                    Icon(
                        imageVector = onboardingPage.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 28.dp)
                            .sizeIn(maxWidth = 96.dp, maxHeight = 96.dp)
                            .size(72.dp)
                    )

                    Text(
                        text = stringResource(onboardingPage.titleRes),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = 20.dp)
                            .semantics { heading() }
                    )

                    Text(
                        text = stringResource(onboardingPage.bodyRes),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Back appears only once there is somewhere to go back to, so
                    // the first page is a single uncluttered action.
                    if (page > 0) {
                        TextButton(
                            onClick = { page -= 1 },
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                        ) {
                            Text(stringResource(R.string.onboarding_back))
                        }
                    }

                    Button(
                        onClick = {
                            // The last page finishes; every other page advances.
                            if (isLastPage) onFinished() else page += 1
                        },
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .testTag(TEST_TAG_ADVANCE)
                    ) {
                        Text(
                            stringResource(
                                if (isLastPage) R.string.onboarding_start
                                else R.string.onboarding_next
                            )
                        )
                    }
                }

                // Skip stays available on every page before the last, and
                // reports through the same callback as finishing the tour, so
                // both routes are recorded identically.
                if (!isLastPage) {
                    TextButton(
                        onClick = onFinished,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag(TEST_TAG_SKIP)
                    ) {
                        Text(stringResource(R.string.onboarding_skip))
                    }
                }
            }
        }
    }
}

/**
 * Dots showing how far through the tour the reader is.
 *
 * The dots carry no label of their own; the whole row is announced once as
 * "Page 2 of 3" instead, which is more useful than three unlabelled nodes and
 * does not repeat itself as a screen reader steps through them.
 */
@Composable
private fun PageIndicator(
    pageCount: Int,
    currentPage: Int,
    modifier: Modifier = Modifier
) {
    val indicatorLabel = stringResource(
        R.string.onboarding_page_indicator,
        currentPage + 1,
        pageCount
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics { contentDescription = indicatorLabel },
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        repeat(pageCount) { index ->
            val isCurrent = index == currentPage
            val color by animateColorAsState(
                targetValue = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                label = "onboardingIndicator"
            )
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color = color, shape = CircleShape)
            )
        }
    }
}

const val TEST_TAG_ONBOARDING = "onboarding_screen"
const val TEST_TAG_ADVANCE = "onboarding_advance"
const val TEST_TAG_SKIP = "onboarding_skip"