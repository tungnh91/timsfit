package com.timsfit.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class OverrideAcceptanceTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val cleanLog = object : ExternalResource() {
        override fun before() {
            val directory = InstrumentationRegistry.getInstrumentation().targetContext.filesDir
            listOf("training-log-v1.json", "training-log-v1.json.bak", "training-log-v1.json.new").forEach { File(directory, it).delete() }
        }
    }
    @get:Rule val rules: RuleChain = RuleChain.outerRule(cleanLog).around(compose)
    private fun awaitText(text: String) = compose.waitUntil(10_000) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }
    private fun savedWorkout() = org.json.JSONObject(File(InstrumentationRegistry.getInstrumentation().targetContext.filesDir,
        "training-log-v1.json").readText()).getJSONArray("workouts").getJSONObject(0)

    @Test fun chosenLegsCompletionSeedsNextChosenLegsAndSuggestsPush() {
        awaitText("Create workout")
        compose.onNodeWithText("Suggested: Push").assertExists()
        compose.onNodeWithText("Legs").performClick()
        compose.onNodeWithText("Create workout").performClick()
        awaitText("Start workout")
        compose.onNodeWithText("Barbell squat").assertExists()
        compose.onNodeWithText("Start workout").performClick()
        awaitText("Save set")
        compose.onNodeWithContentDescription("Barbell squat set 1 weight in lb").performTextReplacement("65")
        compose.onNodeWithContentDescription("Barbell squat set 1 completed").performClick()
        compose.onAllNodesWithText("Save set")[0].performClick()
        awaitText("Saved ✓")
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Finish workout"))
        compose.onNodeWithText("Finish workout").performClick()
        awaitText("Create workout")
        compose.onNodeWithText("Suggested: Push").assertExists()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("View / edit"))
        compose.onNodeWithText("1/12 sets completed").assertExists()
        compose.onNodeWithText("View / edit").performClick()
        awaitText("Legs day")
        compose.onNodeWithContentDescription("Barbell squat set 1 weight in lb").assertTextContains("65.0")
        compose.onNodeWithText("Pull").assertDoesNotExist()
        compose.onNodeWithText("‹  Home").performClick()
        awaitText("Create workout")
        compose.onNodeWithText("Legs").performClick()
        compose.onNodeWithText("Create workout").performClick()
        awaitText("Start workout")
        compose.onNodeWithText("Barbell squat").assertExists()
        compose.onNodeWithText("Start workout").performClick()
        awaitText("Save set")
        compose.onNodeWithContentDescription("Barbell squat set 1 weight in lb").assertTextContains("65.0")
        compose.onNodeWithContentDescription("Barbell squat set 1 completed").assertIsOff()
        compose.onNodeWithText("Pull").assertDoesNotExist()
    }
}
