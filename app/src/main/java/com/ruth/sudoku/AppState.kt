package com.ruth.sudoku

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

class AppTheme(
    val name: String,
    val bg: Color,
    val panel: Color,
    val accent: Color,
    val accent2: Color,
    val unlockedBy: String? = null, // level whose completion unlocks it; null = always
)

val THEMES = listOf(
    AppTheme("Neon", Color(0xFF0D0B1A), Color(0xFF1A1630), Color(0xFFB347FF), Color(0xFF22E5FF)),
    AppTheme("Sunset", Color(0xFF1A0E0B), Color(0xFF301A16), Color(0xFFFF7A45), Color(0xFFFFD166), "Easy"),
    AppTheme("Forest", Color(0xFF0A1A12), Color(0xFF14301F), Color(0xFF2ECC71), Color(0xFFB8F2C8), "Medium"),
    AppTheme("Gold", Color(0xFF14110A), Color(0xFF2A2414), Color(0xFFFFC107), Color(0xFFFFF3B0), "Hard"),
)

val RED = Color(0xFFFF4D6D)

class AppState(context: Context) {
    private val prefs = context.getSharedPreferences("sudoku", Context.MODE_PRIVATE)

    var themeIdx by mutableIntStateOf(prefs.getInt("theme", 0))
        private set
    var alerts by mutableStateOf(prefs.getBoolean("alerts", true))
        private set
    var done by mutableStateOf(prefs.getStringSet("done", emptySet())!!.toSet())
        private set

    val t get() = THEMES[themeIdx]

    init {
        if (!themeUnlocked(t)) themeIdx = 0
    }

    fun themeUnlocked(th: AppTheme) = th.unlockedBy == null || th.unlockedBy in done

    // Level n+1 opens once level n is completed.
    fun levelUnlocked(level: String) = LEVELS.indexOf(level) == 0 || LEVELS[LEVELS.indexOf(level) - 1] in done

    private fun save() = prefs.edit()
        .putInt("theme", themeIdx).putBoolean("alerts", alerts).putStringSet("done", done).apply()

    fun setTheme(i: Int) { themeIdx = i; save() }
    fun enableAlerts(v: Boolean) { alerts = v; save() }
    fun reset() { done = emptySet(); themeIdx = 0; save() }

    /** Records a win; returns the theme it unlocked (and applies it), or null if nothing new. */
    fun complete(level: String): AppTheme? {
        if (level in done) return null
        done = done + level
        val unlocked = THEMES.firstOrNull { it.unlockedBy == level }
        if (unlocked != null) themeIdx = THEMES.indexOf(unlocked)
        save()
        return unlocked
    }
}
