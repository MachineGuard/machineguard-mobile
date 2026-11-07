import 'package:flutter/material.dart';

/// Tokens del Design System (Capítulo V, 5.1.1).
class MgColors {
  static const primary = Color(0xFF0F4C5C);
  static const primaryContainer = Color(0xFFD6EEF2);
  static const ok = Color(0xFF2A9D8F);
  static const warn = Color(0xFFE9A23B);
  static const error = Color(0xFFD62839);
  static const nightNavy = Color(0xFF0B1F2A);
  static const slate = Color(0xFF52667A);
  static const cloud = Color(0xFFF4F8FA);
  static const offline = Color(0xFF8A97A3);
}

class AppTheme {
  static ThemeData light() {
    final scheme = ColorScheme.fromSeed(
      seedColor: MgColors.primary,
      brightness: Brightness.light,
    ).copyWith(
      primary: MgColors.primary,
      primaryContainer: MgColors.primaryContainer,
      secondary: MgColors.ok,
      tertiary: MgColors.warn,
      error: MgColors.error,
      surface: Colors.white,
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: MgColors.cloud,
    );
  }

  static ThemeData dark() {
    final scheme = ColorScheme.fromSeed(
      seedColor: MgColors.primary,
      brightness: Brightness.dark,
    ).copyWith(
      secondary: MgColors.ok,
      tertiary: MgColors.warn,
      error: MgColors.error,
      surface: MgColors.nightNavy,
    );
    return ThemeData(
      useMaterial3: true,
      colorScheme: scheme,
      scaffoldBackgroundColor: MgColors.nightNavy,
    );
  }
}
