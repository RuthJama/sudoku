package com.ruth.sudoku

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GameTest {
    @Test
    fun generatorMakesValidFullGrid() {
        val g = IntArray(81)
        assertTrue(fill(g, Random(1)))
        for (i in 0 until 81) {
            val n = g[i]
            g[i] = 0
            assertTrue(canPlace(g, i, n))
            g[i] = n
        }
    }

    @Test
    fun puzzleHasExpectedHolesAndCorrectEntryIsAccepted() {
        val game = GameState("Easy", Random(2))
        assertEquals(36, game.board.count { it == 0 })
        game.sel = game.board.indexOf(0)
        assertTrue(!game.enter(game.solution[game.sel]))
        assertEquals(0, game.mistakes)
    }
}
