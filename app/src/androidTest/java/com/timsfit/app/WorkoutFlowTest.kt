package com.timsfit.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule
import org.junit.Test

/** Run on a clean app data directory. Exercises the real file-backed app, not a fake state holder. */
class WorkoutFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun createLogFinishAndReopenHistory() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Create next day").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Create next day").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Start workout").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start workout").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Save set").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Weight (lb)")[0].performTextReplacement("25")
        compose.onAllNodesWithText("Reps")[0].performTextReplacement("10")
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onAllNodesWithText("Save set")[0].performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Saved ✓").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Finish workout").performScrollTo().performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Create next day").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("1/12 sets completed").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("View / edit").performClick()
        compose.onNodeWithText("You can edit your saved sets below.").assertIsDisplayed()
        compose.onAllNodesWithText("25.0")[0].assertExists()
    }
}
