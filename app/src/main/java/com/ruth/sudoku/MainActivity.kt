package com.ruth.sudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

private enum class Screen { Splash, Home, Game, Settings }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = AppState(applicationContext)
        setContent {
            val t = app.t
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = t.accent, background = t.bg, surface = t.panel, onSurface = Color.White,
                ),
            ) {
                var screen by remember { mutableStateOf(Screen.Splash) }
                BackHandler(screen == Screen.Game || screen == Screen.Settings) { screen = Screen.Home }
                Box(Modifier.fillMaxSize().background(t.bg).safeDrawingPadding()) {
                    when (screen) {
                        Screen.Splash -> SplashScreen(app) { screen = Screen.Home }
                        Screen.Home -> HomeScreen(app, onPlay = { screen = Screen.Game }, onSettings = { screen = Screen.Settings })
                        Screen.Game -> GameScreen(app)
                        Screen.Settings -> SettingsScreen(app) { screen = Screen.Home }
                    }
                }
            }
        }
    }
}

@Composable
private fun Title(app: AppState, size: Int, spacing: Int) = Text(
    "SUDOKU", color = app.t.accent, fontSize = size.sp, fontWeight = FontWeight.Bold, letterSpacing = spacing.sp,
)

@Composable
private fun SplashScreen(app: AppState, onDone: () -> Unit) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(2200))
        onDone()
    }
    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(140.dp))
        Spacer(Modifier.height(16.dp))
        Title(app, 44, 8)
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(
            progress = { progress.value }, modifier = Modifier.width(200.dp),
            color = app.t.accent2, trackColor = app.t.panel,
        )
        Spacer(Modifier.height(12.dp))
        Text("Loading...", color = Color.White.copy(alpha = .54f))
    }
}

@Composable
private fun HomeScreen(app: AppState, onPlay: () -> Unit, onSettings: () -> Unit) {
    @Composable
    fun menuButton(label: String, onClick: () -> Unit) = OutlinedButton(
        onClick, Modifier.width(220.dp).height(52.dp), border = BorderStroke(1.dp, app.t.accent),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = app.t.accent2),
    ) { Text(label, fontSize = 18.sp) }

    Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
        Image(painterResource(R.drawable.logo), null, Modifier.size(140.dp))
        Spacer(Modifier.height(16.dp))
        Title(app, 40, 8)
        Spacer(Modifier.height(8.dp))
        Text("Theme: ${app.t.name}", color = Color.White.copy(alpha = .54f))
        Spacer(Modifier.height(40.dp))
        menuButton("▶  Play", onPlay)
        Spacer(Modifier.height(16.dp))
        menuButton("⚙  Settings", onSettings)
    }
}

@Composable
private fun SettingsScreen(app: AppState, onBack: () -> Unit) {
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onBack) { Text("←", fontSize = 22.sp, color = app.t.accent2) }
            Text("Settings", fontSize = 22.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text("Theme", fontSize = 18.sp, color = app.t.accent2)
        THEMES.forEachIndexed { i, th ->
            val open = app.themeUnlocked(th)
            Row(
                Modifier.fillMaxWidth().clickable(enabled = open) { app.setTheme(i) }.padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(24.dp).background(th.accent, androidx.compose.foundation.shape.CircleShape))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(th.name, color = if (open) Color.White else Color.White.copy(alpha = .4f))
                    if (!open) Text("Complete ${th.unlockedBy} to unlock", fontSize = 12.sp, color = Color.White.copy(alpha = .4f))
                }
                if (i == app.themeIdx) Text("✔", color = app.t.accent2) else if (!open) Text("🔒")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Wrong number alerts")
                Text("Red flash + vibration on a mistake", fontSize = 12.sp, color = Color.White.copy(alpha = .54f))
            }
            Switch(
                app.alerts, app::enableAlerts,
                colors = SwitchDefaults.colors(checkedTrackColor = app.t.accent2, checkedThumbColor = app.t.bg),
            )
        }
        Column(Modifier.fillMaxWidth().clickable { confirmReset = true }.padding(vertical = 12.dp)) {
            Text("Reset progress", color = RED)
            Text("Re-locks levels and themes", fontSize = 12.sp, color = Color.White.copy(alpha = .54f))
        }
    }
    if (confirmReset) AlertDialog(
        onDismissRequest = { confirmReset = false },
        containerColor = app.t.panel,
        title = { Text("Reset progress?") },
        confirmButton = { TextButton({ app.reset(); confirmReset = false }) { Text("Reset", color = RED) } },
        dismissButton = { TextButton({ confirmReset = false }) { Text("Cancel") } },
    )
}

@Composable
private fun GameScreen(app: AppState) {
    var level by remember { mutableStateOf("Easy") }
    var game by remember { mutableStateOf(GameState(level)) }
    var menu by remember { mutableStateOf(false) }
    var winMsg by remember { mutableStateOf<String?>(null) }
    val haptic = LocalHapticFeedback.current

    fun newGame(l: String = level) { level = l; game = GameState(l) }

    LaunchedEffect(game.flash) {
        if (game.flash >= 0) { delay(500); game.flash = -1 }
    }
    LaunchedEffect(game, game.won) {
        if (!game.won) return@LaunchedEffect
        val unlocked = app.complete(game.level)
        val next = LEVELS.getOrNull(LEVELS.indexOf(game.level) + 1)
        winMsg = listOfNotNull(
            unlocked?.let { "New theme unlocked: ${it.name} (applied!)" },
            next?.let { "$it level is open." },
        ).joinToString("\n")
    }

    fun enter(n: Int) {
        val sel = game.sel
        if (game.enter(n) && app.alerts) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            game.flash = sel
        }
    }

    Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Title(app, 24, 4)
            Box {
                TextButton({ menu = true }) { Text("$level ▾", color = Color.White) }
                DropdownMenu(menu, { menu = false }, containerColor = app.t.panel) {
                    LEVELS.forEach { l ->
                        val open = app.levelUnlocked(l)
                        DropdownMenuItem(
                            text = { Text(if (open) l else "$l 🔒") }, enabled = open,
                            onClick = { menu = false; newGame(l) },
                        )
                    }
                }
            }
        }
        Text("Mistakes ${game.mistakes}/$MAX_MISTAKES", color = if (game.mistakes > 0) RED else Color.White.copy(alpha = .54f))
        Spacer(Modifier.height(8.dp))
        val msg = if (game.won) "Solved! 🎉" else if (game.lost) "Game over" else null
        if (msg != null) Text(msg, fontSize = 20.sp, color = app.t.accent2, modifier = Modifier.padding(bottom = 8.dp))

        Grid(app, game)

        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceEvenly) {
            Tool("⌫", "Erase", app) { enter(0) }
            Tool("✏", if (game.noteMode) "Notes on" else "Notes", app, game.noteMode) { game.noteMode = !game.noteMode }
            Tool("💡", "Hint (${game.hints})", app) { game.hint() }
            Tool("↻", "New", app) { newGame() }
        }
        Spacer(Modifier.weight(1f))
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val w = (maxWidth - 8.dp * 8) / 9
            Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                (1..9).forEach { n ->
                    OutlinedButton(
                        { enter(n) }, Modifier.width(w).height(56.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                        border = BorderStroke(1.dp, app.t.accent),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = app.t.accent2),
                    ) { Text("$n", fontSize = 22.sp) }
                }
            }
        }
        Spacer(Modifier.weight(1f))
    }

    winMsg?.let {
        AlertDialog(
            onDismissRequest = { winMsg = null },
            containerColor = app.t.panel,
            title = { Text("${game.level} complete! 🎉") },
            text = { Text(it) },
            confirmButton = { TextButton({ winMsg = null }) { Text("Nice") } },
        )
    }
}

@Composable
private fun Tool(icon: String, label: String, app: AppState, active: Boolean = false, onClick: () -> Unit) {
    val c = if (active) app.t.accent2 else Color.White.copy(alpha = .7f)
    Column(Modifier.clickable(onClick = onClick).padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp, color = c)
        Text(label, fontSize = 12.sp, color = c)
    }
}

@Composable
private fun Grid(app: AppState, game: GameState) {
    Column(Modifier.fillMaxWidth().aspectRatio(1f).border(2.dp, app.t.accent)) {
        for (r in 0 until 9) Row(Modifier.weight(1f).fillMaxWidth()) {
            for (c in 0 until 9) Box(Modifier.weight(1f).fillMaxHeight()) { Cell(app, game, r * 9 + c) }
        }
    }
}

@Composable
private fun Cell(app: AppState, game: GameState, i: Int) {
    val r = i / 9
    val c = i % 9
    val v = game.board[i]
    val sel = game.sel
    val peer = sel >= 0 && (sel / 9 == r || sel % 9 == c || (sel / 27 == r / 3 && sel % 9 / 3 == c / 3))
    val bg = when {
        i == game.flash -> RED.copy(alpha = .5f)
        game.hl != 0 && v == game.hl -> app.t.accent2.copy(alpha = .35f)
        i == sel -> app.t.accent.copy(alpha = .35f)
        peer -> app.t.panel
        else -> app.t.bg
    }
    val thin = Color.White.copy(alpha = .12f)
    val rightThick = c % 3 == 2 && c < 8
    val bottomThick = r % 3 == 2 && r < 8
    Box(
        Modifier.fillMaxSize().background(bg).clickable { game.tap(i) }.drawBehind {
            val w = 1.5.dp.toPx()
            drawLine(if (rightThick) app.t.accent else thin, Offset(size.width, 0f), Offset(size.width, size.height), w)
            drawLine(if (bottomThick) app.t.accent else thin, Offset(0f, size.height), Offset(size.width, size.height), w)
        },
        contentAlignment = Alignment.Center,
    ) {
        if (v != 0) {
            Text(
                "$v", fontSize = 22.sp,
                fontWeight = if (game.given[i]) FontWeight.Bold else FontWeight.Normal,
                color = when {
                    v != game.solution[i] -> RED
                    game.given[i] -> Color.White
                    else -> app.t.accent2
                },
            )
        } else if (game.notes[i].isNotEmpty()) {
            Column(Modifier.fillMaxSize()) {
                for (row in 0 until 3) Row(Modifier.weight(1f).fillMaxWidth()) {
                    for (col in 0 until 3) {
                        val n = row * 3 + col + 1
                        Text(
                            if (n in game.notes[i]) "$n" else "", Modifier.weight(1f), fontSize = 8.sp,
                            color = Color.White.copy(alpha = .54f), textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
