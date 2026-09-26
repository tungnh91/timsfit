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

class WorkoutChoiceTest {
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

    @Test fun chooseLegsAndSwitchDraftWithoutChangingIdentity() {
        awaitText("TimsFit")
        compose.waitForIdle()
        compose.onNodeWithText("Legs").assertExists().performClick()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Legs").assertIsSelected()
        compose.onNodeWithText("Create workout").performClick()
        awaitText("Legs day")
        awaitText("Start workout")
        compose.waitForIdle()
        val original = savedWorkout()
        assertEquals("LEGS", original.getString("split"))
        compose.onNodeWithText("Pull").performClick()
        awaitText("Pull day")
        compose.waitForIdle()
        val changed = savedWorkout()
        assertEquals("PULL", changed.getString("split"))
        assertEquals(original.getString("id"), changed.getString("id"))
        assertEquals(original.getLong("createdAt"), changed.getLong("createdAt"))
        compose.activityRule.scenario.recreate()
        awaitText("Pull day")
        compose.onNodeWithText("Pull").assertIsSelected()
        compose.onNodeWithText("Push").performClick()
        awaitText("Push day")
        compose.onNodeWithText("Start workout").performClick()
        awaitText("Save set")
        compose.onNodeWithText("Legs").assertDoesNotExist()
        compose.onNodeWithText("‹  Home").performClick()
        awaitText("Resume")
        compose.onNodeWithText("Create workout").assertDoesNotExist()
    }
}
