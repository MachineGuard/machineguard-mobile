import 'package:flutter/material.dart';

import '../../../../shared/widgets/feature_placeholder.dart';

class SettingsPage extends StatelessWidget {
  const SettingsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const FeaturePlaceholder(
      title: 'Settings',
      description: 'Sesión, perfil y cierre de sesión.',
      icon: Icons.settings_outlined,
    );
  }
}
