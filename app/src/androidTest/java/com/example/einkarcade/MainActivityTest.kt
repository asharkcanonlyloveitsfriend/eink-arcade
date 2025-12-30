package com.example.einkarcade

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.click
import com.example.einkarcade.content.LevelSet
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

        val boxOffset = gridOffset(row = 1, col = 2)
        val targetOffset = gridOffset(row = 1, col = 3)
        composeTestRule.onNodeWithTag("gameCanvas").performTouchInput {
            click(boxOffset)
            click(targetOffset)
        }

        composeTestRule
            .onNodeWithText("You win!")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Restart").performTouchInput { click() }

        composeTestRule
            .onNodeWithText("Level 2", substring = true)
            .assertIsDisplayed()
    }
}

private fun gridOffset(row: Int, col: Int): Offset {
    return Offset(
        MainActivity.GRID_OFFSET_X + MainActivity.CELL_SIZE * (col + 0.5f),
        MainActivity.GRID_OFFSET_Y + MainActivity.CELL_SIZE * (row + 0.5f)
    )
}
