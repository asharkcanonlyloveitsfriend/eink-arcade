package com.example.einkarcade

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.einkarcade.content.LevelSet
import com.example.einkarcade.sokoban.Level
import com.example.einkarcade.ui.rendering.geom.BoardViewport
import com.example.einkarcade.ui.rendering.geom.computeBoardViewport
import com.example.einkarcade.ui.rendering.gameBoardTopReservedPx
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
                            id = 1,
                            name = "Training",
                            levels =
                                listOf(
                                    Level.fromAscii(
                                        "Level 1",
                                        """
                                        #####
                                        #@$.#
                                        #####
                                        """.trimIndent(),
                                        puzzleId = 101,
                                    ),
                                    Level.fromAscii(
                                        "Level 2",
                                        """
                                        ######
                                        #@ $.#
                                        ######
                                        """.trimIndent(),
                                        puzzleId = 102,
                                    ),
                                ),
                        ),
                    ),
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
    fun playerSolvesLevelAndAdvancesThroughTheWinOverlay() {
        solveFirstLevel()
        assertSolvedOverlayIsDisplayed()

        composeTestRule.onNodeWithTag("levelSolvedView").performClick()

        finishTransitionAndAssertNextLevel()
    }

    @Test
    fun playerAdvancesThroughTheSolvedLevelForwardButton() {
        solveFirstLevel()
        assertSolvedOverlayIsDisplayed()

        composeTestRule
            .onNodeWithContentDescription("Next level")
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        finishTransitionAndAssertNextLevel()
    }

    private fun solveFirstLevel() {
        composeTestRule
            .onNodeWithText("Level 1", substring = true)
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("gameCanvas").performTouchInput {
            val viewport =
                computeBoardViewport(
                    surfaceWidth = visibleSize.width.toFloat(),
                    surfaceHeight = visibleSize.height.toFloat(),
                    innerRows = 3,
                    innerCols = 5,
                    minimumTopMarginPx = composeTestRule.activity.gameBoardTopReservedPx(),
                )
            click(gridOffsetInMiddleRow(viewport = viewport, col = 2))
            click(gridOffsetInMiddleRow(viewport = viewport, col = 3))
        }
    }

    private fun assertSolvedOverlayIsDisplayed() {
        composeTestRule
            .onNodeWithTag("levelSolvedView")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("You win! Moves: 1!")
            .assertIsDisplayed()
    }

    private fun finishTransitionAndAssertNextLevel() {
        composeTestRule.onNodeWithTag("gameCanvas").performTouchInput {
            click(
                Offset(
                    x = visibleSize.width * 0.95f,
                    y = visibleSize.height * 0.1f,
                ),
            )
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Level 2", substring = true).assertIsDisplayed()
    }
}

private fun gridOffsetInMiddleRow(
    viewport: BoardViewport,
    col: Int,
): Offset =
    Offset(
        viewport.cellLeft(col) + viewport.cellSize / 2f,
        viewport.cellTop(1) + viewport.cellSize / 2f,
    )
