import 'dart:math';
import 'package:flutter_test/flutter_test.dart';
import 'package:sudoku/main.dart';

void main() {
  test('generator makes a valid full grid', () {
    final g = List.filled(81, 0);
    expect(fill(g, Random(1)), isTrue);
    for (var i = 0; i < 81; i++) {
      final n = g[i];
      g[i] = 0;
      expect(canPlace(g, i, n), isTrue);
      g[i] = n;
    }
  });
}
