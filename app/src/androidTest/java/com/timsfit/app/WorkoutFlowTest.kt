package com.timsfit.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

/** Run on a clean app data directory. Exercises the real file-backed app, not a fake state holder. */
class WorkoutFlowTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private val cleanLog = object : ExternalResource() {
        override fun before() {
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
            listOf("training-log-v1.json", "training-log-v1.json.bak", "training-log-v1.json.new").forEach {
                java.io.File(directory, it).delete()
            }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(cleanLog).around(compose)

    @Test fun createLogFinishAndReopenHistory() {
        compose.waitUntil(10_000) { compose.onAllNodesWithText("Create workout").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Create workout").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Start workout").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Start workout").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Save set").fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodesWithText("Weight (lb)")[0].performTextReplacement("25")
        compose.onAllNodesWithText("Reps")[0].performTextReplacement("10")
        compose.onAllNodes(isToggleable())[0].performClick()
        compose.onAllNodesWithText("Save set")[0].performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Saved ✓").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Finish workout"))
        compose.onNodeWithText("Finish workout").performClick()
        compose.waitUntil(5_000) { compose.onAllNodesWithText("Create workout").fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("1/12 sets completed"))
        compose.onNodeWithText("1/12 sets completed").assertIsDisplayed()
        compose.onNodeWithText("View / edit").performClick()
        compose.onNodeWithText("Completed ", substring = true).assertIsDisplayed()
        compose.onAllNodesWithText("25.0")[0].assertExists()
    }
}
