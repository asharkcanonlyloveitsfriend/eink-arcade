package com.example.einkarcade

import android.view.KeyEvent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.example.einkarcade.sokoban.Level
import org.junit.After
import org.junit.Rule
import org.junit.Test

class MainActivityTest {

    companion object {
        init {
            MainActivity.gameControllerFactory = { ctx ->
                GameController(
                    ctx,
                    listOf(
                        LevelSet(
                            id = "training",
                            name = "Training",
                            levels = listOf(
                                Level.fromAscii(
                                    "Level 1",
                                    """
                                    ####
                                    #@$.#
                                    ####
                                    """.trimIndent()
                                ),
                                Level.fromAscii(
                                    "Level 2",
                                    """
                                    #####
                                    #@ $.#
                                    #####
                                    """.trimIndent()
                                )
                            )
                        )
                    )
                )
            }
        }
    }
    @After
    fun tearDown() {
        MainActivity.gameControllerFactory = null
    }

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun playerSolvesLevelAndAdvancesToNextLevel() {
        composeTestRule
            .onNodeWithText("Level 1", substring = true)
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
            .onNodeWithText("Level 2", substring = true)
            .assertIsDisplayed()
    }
}

private fun simulateKeyPress(activity: MainActivity, keyCode: Int) {
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
    activity.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
}