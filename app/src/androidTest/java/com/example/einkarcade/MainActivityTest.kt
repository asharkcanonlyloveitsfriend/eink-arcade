package com.example.einkarcade

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    companion object {
        init {
            MainActivity.testLevels = listOf(
                """
                ####
                #@$.#
                ####
                """.trimIndent(),
                """
                #####
                #@ $.#
                #####
                """.trimIndent()
            )
        }
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun playerSolvesLevelAndAdvancesToNextLevel() {
        composeTestRule
            .onNodeWithText("Level 1")
            .assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            simulateKeyPress(activity, KeyEvent.KEYCODE_DPAD_RIGHT)
        }

        composeTestRule
            .onNodeWithText("You win!")
            .assertIsDisplayed()

        composeTestRule.activityRule.scenario.onActivity { activity ->
            simulateKeyPress(activity, KeyEvent.KEYCODE_BUTTON_X)
        }

        composeTestRule
            .onNodeWithText("Level 2")
            .assertIsDisplayed()
    }
}

private fun simulateKeyPress(activity: MainActivity, keyCode: Int) {
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}