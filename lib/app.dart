import 'package:flutter/material.dart';

import 'app_shell.dart';
import 'core/theme/app_theme.dart';

class MachineGuardApp extends StatelessWidget {
  const MachineGuardApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'MachineGuard',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.light(),
      darkTheme: AppTheme.dark(),
      themeMode: ThemeMode.system,
      home: const AppShell(),
    );
  }
}
