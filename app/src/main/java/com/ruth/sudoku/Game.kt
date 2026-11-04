package com.ruth.sudoku

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.random.Random

const val MAX_MISTAKES = 3
val LEVELS = listOf("Easy", "Medium", "Hard")
private val HOLES = mapOf("Easy" to 36, "Medium" to 46, "Hard" to 54)

fun canPlace(g: IntArray, i: Int, n: Int): Boolean {
    val row = i / 9
    val col = i % 9
    for (k in 0 until 9) {
        if (g[row * 9 + k] == n || g[k * 9 + col] == n) return false
    }
    val br = row / 3 * 3
    val bc = col / 3 * 3
    for (a in 0 until 3) for (b in 0 until 3) {
        if (g[(br + a) * 9 + bc + b] == n) return false
    }
    return true
}

/** Fills [g] with a full valid grid by randomized backtracking. */
fun fill(g: IntArray, rng: Random): Boolean {
    val i = g.indexOf(0)
    if (i < 0) return true
    for (n in (1..9).shuffled(rng)) {
        if (canPlace(g, i, n)) {
            g[i] = n
            if (fill(g, rng)) return true
            g[i] = 0
        }
    }
    return false
}

// ponytail: holes are punched without a uniqueness check, so a puzzle may have
// several solutions; entries are validated against the stored solution. Add a
// solution counter if strict uniqueness matters.
class GameState(val level: String, rng: Random = Random.Default) {
    val solution = IntArray(81).also { fill(it, rng) }
    val board = mutableStateListOf<Int>().apply { addAll(solution.toList()) }
    val notes = mutableStateListOf<Set<Int>>().apply { repeat(81) { add(emptySet()) } }
    val given: BooleanArray

    var sel by mutableIntStateOf(-1)
    var hl by mutableIntStateOf(0) // number whose cells are lit up
    var mistakes by mutableIntStateOf(0)
    var hints by mutableIntStateOf(3)
    var noteMode by mutableStateOf(false)
    var flash by mutableIntStateOf(-1) // cell briefly red after a wrong entry

    init {
        (0 until 81).shuffled(rng).take(HOLES.getValue(level)).forEach { board[it] = 0 }
        given = BooleanArray(81) { board[it] != 0 }
    }

    val lost get() = mistakes >= MAX_MISTAKES
    val won get() = !lost && (0 until 81).all { board[it] == solution[it] }

    fun tap(i: Int) {
        sel = i
        if (board[i] != 0) hl = board[i]
    }

    /** Enters [n] (0 = erase) into the selected box; returns true if it was a wrong number. */
    fun enter(n: Int): Boolean {
        if (n != 0) hl = n
        if (sel < 0 || given[sel] || lost || won) return false
        when {
            noteMode -> if (board[sel] == 0) notes[sel] = notes[sel].let { if (n in it) it - n else it + n }
            n == 0 -> board[sel] = 0
            else -> {
                board[sel] = n
                notes[sel] = emptySet()
                if (n != solution[sel]) {
                    mistakes++
                    return true
                }
            }
        }
        return false
    }

    fun hint() {
        if (hints == 0 || sel < 0 || given[sel] || board[sel] == solution[sel] || lost || won) return
        board[sel] = solution[sel]
        notes[sel] = emptySet()
        hints--
    }
}
