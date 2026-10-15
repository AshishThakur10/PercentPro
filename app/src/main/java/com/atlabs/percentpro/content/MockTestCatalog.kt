package com.atlabs.percentpro.content

import com.atlabs.percentpro.R
import com.atlabs.percentpro.model.MockTestConfig
import com.atlabs.percentpro.model.MockTestId

/**
 * The mock tests shipped with the app.
 *
 * This is a percentage-topic practice paper, not a reproduction of any SSC,
 * Railway or banking examination, and the copy says so explicitly.
 */
class MockTestCatalog {

    fun tests(): List<MockTestConfig> = TESTS

    fun testById(id: MockTestId): MockTestConfig? = tests().firstOrNull { it.id == id }

    private companion object {

        const val FIFTEEN_MINUTES = 15 * 60 * 1000L

        val TESTS = listOf(
            MockTestConfig(
                id = MockTestId("percentage_aptitude_15"),
                titleRes = R.string.mock_title_percentage_aptitude,
                descriptionRes = R.string.mock_description_percentage_aptitude,
                questionCount = 15,
                durationMillis = FIFTEEN_MINUTES,
                // +1 mark correct, -0.25 mark incorrect.
                marksPerCorrectInQuarters = 4,
                negativeMarkInQuarters = 1
            )
        )
    }
}