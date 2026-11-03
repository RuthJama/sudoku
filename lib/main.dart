import 'dart:async';
import 'dart:math';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:shared_preferences/shared_preferences.dart';

const red = Color(0xFFFF4D6D);
const maxMistakes = 3;

class AppTheme {
  final String name;
  final Color bg, panel, accent, accent2;
  final String? unlockedBy; // level whose completion unlocks it; null = always
  const AppTheme(this.name, this.bg, this.panel, this.accent, this.accent2, [this.unlockedBy]);
}

const themes = [
  AppTheme('Neon', Color(0xFF0D0B1A), Color(0xFF1A1630), Color(0xFFB347FF), Color(0xFF22E5FF)),
  AppTheme('Sunset', Color(0xFF1A0E0B), Color(0xFF301A16), Color(0xFFFF7A45), Color(0xFFFFD166), 'Easy'),
  AppTheme('Forest', Color(0xFF0A1A12), Color(0xFF14301F), Color(0xFF2ECC71), Color(0xFFB8F2C8), 'Medium'),
  AppTheme('Gold', Color(0xFF14110A), Color(0xFF2A2414), Color(0xFFFFC107), Color(0xFFFFF3B0), 'Hard'),
];
const levels = ['Easy', 'Medium', 'Hard'];

class AppState extends ChangeNotifier {
  SharedPreferences? _p;
  int themeIdx = 0;
  bool alerts = true;
  Set<String> done = {};

  AppTheme get t => themes[themeIdx];
  bool themeUnlocked(AppTheme th) => th.unlockedBy == null || done.contains(th.unlockedBy);
  // Level n+1 opens once level n is completed.
  bool levelUnlocked(String l) => levels.indexOf(l) == 0 || done.contains(levels[levels.indexOf(l) - 1]);

  Future<void> load() async {
    _p = await SharedPreferences.getInstance();
    themeIdx = _p!.getInt('theme') ?? 0;
    alerts = _p!.getBool('alerts') ?? true;
    done = (_p!.getStringList('done') ?? []).toSet();
    if (!themeUnlocked(t)) themeIdx = 0;
  }

  void _save() {
    _p?.setInt('theme', themeIdx);
    _p?.setBool('alerts', alerts);
    _p?.setStringList('done', done.toList());
    notifyListeners();
  }

  void setTheme(int i) {
    themeIdx = i;
    _save();
  }

  void setAlerts(bool v) {
    alerts = v;
    _save();
  }

  void reset() {
    done = {};
    themeIdx = 0;
    _save();
  }

  /// Returns the theme this win unlocked (and applies it), or null if nothing new.
  AppTheme? complete(String level) {
    if (!done.add(level)) return null;
    final unlocked = themes.where((th) => th.unlockedBy == level).firstOrNull;
    if (unlocked != null) themeIdx = themes.indexOf(unlocked);
    _save();
    return unlocked;
  }
}

final app = AppState();

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  await app.load();
  runApp(const SudokuApp());
}

class SudokuApp extends StatelessWidget {
  const SudokuApp({super.key});
  @override
  Widget build(BuildContext c) => ListenableBuilder(
        listenable: app,
        builder: (_, __) => MaterialApp(
          title: 'Sudoku',
          debugShowCheckedModeBanner: false,
          theme: ThemeData.dark().copyWith(scaffoldBackgroundColor: app.t.bg),
          home: const SplashPage(),
        ),
      );
}

class SplashPage extends StatefulWidget {
  const SplashPage({super.key});
  @override
  State<SplashPage> createState() => _SplashPageState();
}

class _SplashPageState extends State<SplashPage> with SingleTickerProviderStateMixin {
  late final ctl = AnimationController(vsync: this, duration: const Duration(milliseconds: 2200))..forward();

  @override
  void initState() {
    super.initState();
    ctl.addStatusListener((s) {
      if (s == AnimationStatus.completed && mounted) {
        Navigator.of(context).pushReplacement(MaterialPageRoute(builder: (_) => const HomePage()));
      }
    });
  }

  @override
  void dispose() {
    ctl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
        body: Center(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Image.asset('assets/logo.png', width: 140),
            const SizedBox(height: 16),
            Text('SUDOKU',
                style: TextStyle(fontSize: 44, fontWeight: FontWeight.bold, color: app.t.accent, letterSpacing: 8)),
            const SizedBox(height: 32),
            SizedBox(
              width: 200,
              child: AnimatedBuilder(
                animation: ctl,
                builder: (_, __) => LinearProgressIndicator(
                  value: ctl.value,
                  color: app.t.accent2,
                  backgroundColor: app.t.panel,
                ),
              ),
            ),
            const SizedBox(height: 12),
            const Text('Loading...', style: TextStyle(color: Colors.white54)),
          ]),
        ),
      );
}

class HomePage extends StatelessWidget {
  const HomePage({super.key});

  Widget btn(BuildContext c, IconData icon, String label, Widget page) => SizedBox(
        width: 220,
        height: 52,
        child: OutlinedButton.icon(
          style: OutlinedButton.styleFrom(side: BorderSide(color: app.t.accent), foregroundColor: app.t.accent2),
          icon: Icon(icon),
          label: Text(label, style: const TextStyle(fontSize: 18)),
          onPressed: () => Navigator.of(c).push(MaterialPageRoute(builder: (_) => page)),
        ),
      );

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: app,
        builder: (c, _) => Scaffold(
          body: Center(
            child: Column(mainAxisSize: MainAxisSize.min, children: [
              Image.asset('assets/logo.png', width: 140),
              const SizedBox(height: 16),
              Text('SUDOKU',
                  style: TextStyle(fontSize: 40, fontWeight: FontWeight.bold, color: app.t.accent, letterSpacing: 8)),
              const SizedBox(height: 8),
              Text('Theme: ${app.t.name}', style: const TextStyle(color: Colors.white54)),
              const SizedBox(height: 40),
              btn(c, Icons.play_arrow, 'Play', const GamePage()),
              const SizedBox(height: 16),
              btn(c, Icons.settings, 'Settings', const SettingsPage()),
            ]),
          ),
        ),
      );
}

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) => ListenableBuilder(
        listenable: app,
        builder: (c, _) => Scaffold(
          appBar: AppBar(title: const Text('Settings'), backgroundColor: app.t.panel),
          body: ListView(padding: const EdgeInsets.all(16), children: [
            Text('Theme', style: TextStyle(fontSize: 18, color: app.t.accent2)),
            const SizedBox(height: 8),
            for (var i = 0; i < themes.length; i++)
              ListTile(
                enabled: app.themeUnlocked(themes[i]),
                leading: CircleAvatar(backgroundColor: themes[i].accent, radius: 12),
                title: Text(themes[i].name),
                subtitle: app.themeUnlocked(themes[i]) ? null : Text('Complete ${themes[i].unlockedBy} to unlock'),
                trailing: i == app.themeIdx
                    ? Icon(Icons.check_circle, color: app.t.accent2)
                    : app.themeUnlocked(themes[i])
                        ? null
                        : const Icon(Icons.lock, size: 18),
                onTap: () => app.setTheme(i),
              ),
            const Divider(),
            SwitchListTile(
              title: const Text('Wrong number alerts'),
              subtitle: const Text('Red flash + vibration on a mistake'),
              value: app.alerts,
              activeColor: app.t.accent2,
              onChanged: app.setAlerts,
            ),
            ListTile(
              leading: const Icon(Icons.restart_alt, color: red),
              title: const Text('Reset progress'),
              subtitle: const Text('Re-locks levels and themes'),
              onTap: () => showDialog(
                context: c,
                builder: (d) => AlertDialog(
                  title: const Text('Reset progress?'),
                  actions: [
                    TextButton(onPressed: () => Navigator.pop(d), child: const Text('Cancel')),
                    TextButton(
                      onPressed: () {
                        app.reset();
                        Navigator.pop(d);
                      },
                      child: const Text('Reset', style: TextStyle(color: red)),
                    ),
                  ],
                ),
              ),
            ),
          ]),
        ),
      );
}

/// Fills a full valid grid by randomized backtracking.
bool fill(List<int> g, Random r) {
  final i = g.indexOf(0);
  if (i < 0) return true;
  final nums = [1, 2, 3, 4, 5, 6, 7, 8, 9]..shuffle(r);
  for (final n in nums) {
    if (canPlace(g, i, n)) {
      g[i] = n;
      if (fill(g, r)) return true;
      g[i] = 0;
    }
  }
  return false;
}

bool canPlace(List<int> g, int i, int n) {
  final row = i ~/ 9, col = i % 9;
  for (var k = 0; k < 9; k++) {
    if (g[row * 9 + k] == n || g[k * 9 + col] == n) return false;
  }
  final br = row ~/ 3 * 3, bc = col ~/ 3 * 3;
  for (var a = 0; a < 3; a++) {
    for (var b = 0; b < 3; b++) {
      if (g[(br + a) * 9 + bc + b] == n) return false;
    }
  }
  return true;
}

// ponytail: removes cells without a uniqueness check, so a puzzle may have
// several solutions; we validate against the stored solution. Add a solver
// count if strict uniqueness matters.
const holes = {'Easy': 36, 'Medium': 46, 'Hard': 54};

class GamePage extends StatefulWidget {
  const GamePage({super.key});
  @override
  State<GamePage> createState() => _GamePageState();
}

class _GamePageState extends State<GamePage> {
  final rng = Random();
  String level = 'Easy';
  late List<int> solution, board;
  late List<bool> given;
  late List<Set<int>> notes;
  int sel = -1, mistakes = 0, hints = 3;
  bool noteMode = false;
  int hl = 0; // number whose cells are lit up
  int flash = -1; // cell briefly highlighted after a wrong entry

  @override
  void initState() {
    super.initState();
    newGame();
  }

  void newGame() {
    solution = List.filled(81, 0);
    fill(solution, rng);
    board = [...solution];
    final cells = List.generate(81, (i) => i)..shuffle(rng);
    for (final i in cells.take(holes[level]!)) {
      board[i] = 0;
    }
    given = board.map((v) => v != 0).toList();
    notes = List.generate(81, (_) => <int>{});
    sel = -1;
    hl = 0;
    mistakes = 0;
    hints = 3;
    setState(() {});
  }

  bool get lost => mistakes >= maxMistakes;
  bool get won => !lost && List.generate(81, (i) => board[i] == solution[i]).every((e) => e);

  /// Numbers that fit the selected empty/editable box (no clash in row, column or block).
  List<int> get keys {
    final all = [1, 2, 3, 4, 5, 6, 7, 8, 9];
    if (sel < 0 || given[sel]) return all;
    final tmp = [...board]..[sel] = 0;
    return all.where((n) => canPlace(tmp, sel, n)).toList();
  }

  void enter(int n) {
    if (n != 0) setState(() => hl = n);
    if (sel < 0 || given[sel] || lost || won) return;
    setState(() {
      if (noteMode) {
        if (board[sel] == 0) notes[sel].contains(n) ? notes[sel].remove(n) : notes[sel].add(n);
      } else if (n == 0) {
        board[sel] = 0;
      } else {
        board[sel] = n;
        notes[sel].clear();
        if (n != solution[sel]) wrong(sel);
      }
    });
    if (won) finish();
  }

  void wrong(int cell) {
    mistakes++;
    if (!app.alerts) return;
    HapticFeedback.heavyImpact();
    flash = cell;
    Timer(const Duration(milliseconds: 500), () => mounted ? setState(() => flash = -1) : null);
  }

  void finish() {
    final unlockedTheme = app.complete(level);
    final next = levels.indexOf(level) + 1 < levels.length ? levels[levels.indexOf(level) + 1] : null;
    showDialog(
      context: context,
      builder: (d) => AlertDialog(
        backgroundColor: app.t.panel,
        title: Text('$level complete! 🎉'),
        content: Text([
          if (unlockedTheme != null) 'New theme unlocked: ${unlockedTheme.name} (applied!)',
          if (next != null) '$next level is open.',
        ].join('\n')),
        actions: [TextButton(onPressed: () => Navigator.pop(d), child: const Text('Nice'))],
      ),
    );
  }

  void hint() {
    if (hints == 0 || sel < 0 || given[sel] || board[sel] == solution[sel] || lost || won) return;
    setState(() {
      board[sel] = solution[sel];
      notes[sel].clear();
      hints--;
    });
    if (won) finish();
  }

  @override
  Widget build(BuildContext context) {
    final msg = won ? 'Solved! 🎉' : lost ? 'Game over' : null;
    return Scaffold(
      body: SafeArea(
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(children: [
            Row(mainAxisAlignment: MainAxisAlignment.spaceBetween, children: [
              Text('SUDOKU',
                  style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: app.t.accent, letterSpacing: 4)),
              DropdownButton<String>(
                value: level,
                dropdownColor: app.t.panel,
                items: levels
                    .map((k) => DropdownMenuItem(
                        value: k, enabled: app.levelUnlocked(k), child: Text(app.levelUnlocked(k) ? k : '$k 🔒')))
                    .toList(),
                onChanged: (v) {
                  level = v!;
                  newGame();
                },
              ),
            ]),
            Text('Mistakes $mistakes/$maxMistakes', style: TextStyle(color: mistakes > 0 ? red : Colors.white54)),
            const SizedBox(height: 8),
            if (msg != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Text(msg, style: TextStyle(fontSize: 20, color: app.t.accent2)),
              ),
            AspectRatio(aspectRatio: 1, child: grid()),
            const SizedBox(height: 12),
            Row(mainAxisAlignment: MainAxisAlignment.spaceEvenly, children: [
              tool(Icons.undo, 'Erase', () => enter(0)),
              tool(Icons.edit, noteMode ? 'Notes on' : 'Notes', () => setState(() => noteMode = !noteMode),
                  active: noteMode),
              tool(Icons.lightbulb, 'Hint ($hints)', hint),
              tool(Icons.refresh, 'New', newGame),
            ]),
            const Spacer(),
            Wrap(
              alignment: WrapAlignment.center,
              spacing: 8,
              children: [for (final n in keys) numKey(n)],
            ),
            const Spacer(),
          ]),
        ),
      ),
    );
  }

  Widget tool(IconData icon, String label, VoidCallback f, {bool active = false}) => InkWell(
        onTap: f,
        child: Column(children: [
          Icon(icon, color: active ? app.t.accent2 : Colors.white70),
          Text(label, style: TextStyle(fontSize: 12, color: active ? app.t.accent2 : Colors.white54)),
        ]),
      );

  Widget numKey(int n) => SizedBox(
        width: 56,
        height: 56,
        child: OutlinedButton(
          style: OutlinedButton.styleFrom(side: BorderSide(color: app.t.accent), foregroundColor: app.t.accent2),
          onPressed: () => enter(n),
          child: Text('$n', style: const TextStyle(fontSize: 22)),
        ),
      );

  Widget grid() => Container(
        decoration: BoxDecoration(border: Border.all(color: app.t.accent, width: 2)),
        child: Column(children: [
          for (var r = 0; r < 9; r++)
            Expanded(child: Row(children: [for (var c = 0; c < 9; c++) Expanded(child: cell(r * 9 + c))])),
        ]),
      );

  Widget cell(int i) {
    final r = i ~/ 9, c = i % 9;
    final v = board[i];
    final selected = i == sel;
    final peer = sel >= 0 &&
        (sel ~/ 9 == r || sel % 9 == c || (sel ~/ 27 == r ~/ 3 && sel % 9 ~/ 3 == c ~/ 3));
    final wrong = v != 0 && v != solution[i];
    final thin = const BorderSide(color: Colors.white12), thick = BorderSide(color: app.t.accent, width: 1.5);
    return GestureDetector(
      onTap: () => setState(() {
        sel = i;
        if (board[i] != 0) hl = board[i];
      }),
      child: Container(
        decoration: BoxDecoration(
          color: i == flash ? red.withOpacity(.5) : hl != 0 && v == hl ? app.t.accent2.withOpacity(.35) : selected ? app.t.accent.withOpacity(.35) : peer ? app.t.panel : app.t.bg,
          border: Border(
            right: c % 3 == 2 && c < 8 ? thick : thin,
            bottom: r % 3 == 2 && r < 8 ? thick : thin,
          ),
        ),
        alignment: Alignment.center,
        child: v != 0
            ? Text('$v',
                style: TextStyle(
                  fontSize: 22,
                  fontWeight: given[i] ? FontWeight.bold : FontWeight.normal,
                  color: wrong ? red : given[i] ? Colors.white : app.t.accent2,
                ))
            : notes[i].isEmpty
                ? null
                : GridView.count(
                    crossAxisCount: 3,
                    physics: const NeverScrollableScrollPhysics(),
                    children: [
                      for (var n = 1; n <= 9; n++)
                        Center(
                          child: Text(notes[i].contains(n) ? '$n' : '',
                              style: const TextStyle(fontSize: 8, color: Colors.white54)),
                        ),
                    ],
                  ),
      ),
    );
  }
}
