package com.timsfit.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test

class AutosaveAcceptanceTest {
    val compose = createAndroidComposeRule<MainActivity>()
    private val clean = object : ExternalResource() {
        override fun before() {
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
            listOf("training-log-v1.json", "training-log-v1.json.bak", "training-log-v1.json.new").forEach { java.io.File(directory, it).delete() }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(clean).around(compose)
    private fun waitFor(text: String) = compose.waitUntil(10_000) { compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty() }
    @Test fun editAutosavesWithoutCompletionControlAndSurvivesRecreation() {
        waitFor("Create workout")
        compose.onNodeWithText("Create workout").performClick()
        waitFor("Start workout")
        compose.onNodeWithText("Start workout").performClick()
        waitFor("Weight (lb)")
        compose.onAllNodesWithText("Save set").assertCountEquals(0)
        compose.onAllNodes(isToggleable()).assertCountEquals(0)
        compose.onNodeWithText("0 of 12 sets completed").assertExists()
        compose.onNodeWithContentDescription("Demo for Barbell bench press. Opens browser; internet required.").assertExists()
        compose.onNodeWithText("Elapsed", substring = true).assertExists()
        val weight = compose.onNodeWithContentDescription("Barbell bench press set 1 weight in lb")
        weight.assertTextContains("0")
        weight.performTextReplacement("3")
        weight.performTextInput("0")
        waitFor("Saved")
        compose.activityRule.scenario.recreate()
        waitFor("Weight (lb)")
        weight.assertTextContains("30")
        compose.onNodeWithText("1 of 12 sets completed").assertExists()
        weight.performTextReplacement("32.")
        waitFor("Saved")
        weight.assertTextContains("32.")
        weight.performTextInput("5")
        waitFor("Saved")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Finish workout"))
        compose.onNodeWithText("Finish workout").performClick()
        waitFor("Create workout")
        compose.onNodeWithText("Push", substring = false).performClick()
        compose.onNodeWithText("Create workout").performClick()
        waitFor("Start workout")
        compose.onNodeWithText("32.5 / 0 / 0 lb").assertExists()
    }
}
