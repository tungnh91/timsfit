package com.timsfit.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

/** Run alone on clean app data; checks behavior beyond the happy-path smoke test. */
class IndependentFlowTest {
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
    private fun waitFor(text: String) = compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private val weight = "Barbell bench press set 1 weight in lb"

    @Test fun invalidEditsAndUnsavedDraftSurviveRecreationBeforeDurableSave() {
        waitFor("Create next day")
        compose.onNodeWithText("Create next day").performClick()
        waitFor("Start workout")
        compose.onNodeWithText("Start workout").performClick()
        waitFor("Save set")
        compose.onNodeWithContentDescription(weight).performTextReplacement("-1")
        compose.onAllNodesWithText("Save set")[0].performClick()
        compose.onNodeWithText("Enter 0–1500 lb and 1–100 reps.").assertExists()
        compose.onNodeWithContentDescription(weight).performTextReplacement("42.5")
        compose.activityRule.scenario.recreate()
        waitFor("Save set")
        compose.onNodeWithContentDescription(weight).assertTextContains("42.5")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("‹  Home"))
        compose.onNodeWithText("‹  Home").performClick()
        compose.onNodeWithText("Unsaved set changes").assertExists()
        compose.onNodeWithText("Keep editing").performClick()
        compose.onNodeWithContentDescription("Barbell bench press set 1 completed").performClick()
        compose.onAllNodesWithText("Save set")[0].performClick()
        waitFor("Saved ✓")
        compose.activityRule.scenario.recreate()
        waitFor("Saved ✓")
        compose.onNodeWithContentDescription(weight).assertTextContains("42.5")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Finish workout"))
        compose.onNodeWithText("Finish workout").performClick()
        waitFor("Create next day")
        compose.onNodeWithText("Pull day").assertExists()
    }
}
