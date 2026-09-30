package com.timsfit.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

class IndependentAutosaveTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private val clean = object : ExternalResource() {
        override fun before() {
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
            listOf("training-log-v1.json", "training-log-v1.json.bak", "training-log-v1.json.new").forEach { java.io.File(directory, it).delete() }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(clean).around(compose)
    private fun waitFor(text: String) = compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    @Test fun invalidDraftSurvivesRecreationAndFinishKeepsLastValidAutosave() {
        waitFor("Create workout")
        compose.onNodeWithText("Create workout").performClick()
        waitFor("Start workout")
        compose.onNodeWithText("Start workout").performClick()
        waitFor("Weight (lb)")
        val weight = compose.onNodeWithContentDescription("Barbell bench press set 1 weight in lb")
        val reps = compose.onNodeWithContentDescription("Barbell bench press set 1 reps")
        weight.performTextReplacement("30")
        reps.performTextReplacement("7")
        waitFor("Saved")
        reps.performTextReplacement("")
        compose.onNodeWithText("Not saved").assertExists()
        compose.activityRule.scenario.recreate()
        waitFor("Weight (lb)")
        compose.onNodeWithText("Not saved").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Finish workout"))
        compose.onNodeWithText("Finish workout").performClick()
        compose.onNodeWithText("Unsaved set changes").assertExists()
        compose.onNodeWithText("Continue without changes").performClick()
        waitFor("Create workout")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("View / edit"))
        compose.onNodeWithText("1/12 sets completed").assertExists()
        compose.onNodeWithText("View / edit").performClick()
        waitFor("Weight (lb)")
        weight.assertTextContains("30")
        reps.assertTextContains("7")
        compose.onAllNodesWithText("Save set").assertCountEquals(0)
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
    }
}
