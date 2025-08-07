package com.example.einkarcade.sokoban

import org.junit.Assert.*
import org.junit.Test

class BoxMoverTest {

    data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    @Test
    fun testCanMoveBox_StraightLineWithPlayerAccess() {
        val asciiMap = """
            #######
            #@    #
            # $  x#
            #######
        """.trimIndent()
        val (mover, playerPosition, to, boxPosition) = parseBoxMoverWithEndpoints(asciiMap)

        val expectedFinalPlayerPos = Position(2, 4) // left of 'x'
        assertEquals(expectedFinalPlayerPos, mover.canMoveBox(boxPosition, to, playerPosition))
    }
    @Test
    fun testCanMoveBox_NotStraightLine() {
        val asciiMap = """
            #####
            #@  #
            # $ #
            #  x#
            #####
        """.trimIndent()
        val (mover, playerPosition, to, boxPosition) = parseBoxMoverWithEndpoints(asciiMap)

        val expectedFinalPlayerPos = Position(3, 2) // left of 'x'
        assertEquals(expectedFinalPlayerPos, mover.canMoveBox(boxPosition, to, playerPosition))
    }
    @Test
    fun testCanMoveBox_ComplexPath() {
        val asciiMap = """
            ###################
            # ###   ##        #
            # #  $#  #        #
            ### # ## #   ######
            #   # ## ## ##    #
            # #              ##
            #    ####    @#  x#
            ###################
        """.trimIndent()
        val (mover, playerPosition, to, boxPosition) = parseBoxMoverWithEndpoints(asciiMap)

        val expectedFinalPlayerPos = Position(6, 16) // last push was from left of x
        assertEquals(expectedFinalPlayerPos, mover.canMoveBox(boxPosition, to, playerPosition))
    }
    @Test
    fun testCanMoveBox_Blocked() {
        val asciiMap = """
            #####
            #   #
            ###$#
            #  @#
            # #x#
            #####
        """.trimIndent()
        val (mover, playerPosition, to, boxPosition) = parseBoxMoverWithEndpoints(asciiMap)

        assertNull(mover.canMoveBox(boxPosition, to, playerPosition))
    }

    private fun parseBoxMoverWithEndpoints(asciiMap: String): Quadruple<BoxMover, Position, Position, Position> {
        var player: Position? = null
        var to: Position? = null
        var box: Position? = null

        val grid = asciiMap.lines().mapIndexed { rowIndex, row ->
            row.mapIndexed { colIndex, char ->
                when (char) {
                    '@' -> {
                        player = Position(rowIndex, colIndex)
                        true
                    }
                    'x' -> {
                        to = Position(rowIndex, colIndex)
                        true
                    }
                    '$' -> {
                        box = Position(rowIndex, colIndex)
                        true
                    }
                    '#' -> false
                    ' ' -> true
                    else -> error("Unsupported character: $char")
                }
            }.toTypedArray()
        }.toTypedArray()

        require(player != null && to != null && box != null) {
            "Map must contain '@', 'x', and '\$'."
        }

        val mover = BoxMover(grid)
        return Quadruple(mover, player!!, to!!, box!!)
    }

}