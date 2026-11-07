import 'package:flutter/material.dart';

import '../../../../shared/widgets/feature_placeholder.dart';

class DashboardPage extends StatelessWidget {
  const DashboardPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const FeaturePlaceholder(
      title: 'Dashboard',
      description: 'Zonas y lecturas actuales de temperatura y humedad.',
      icon: Icons.dashboard_outlined,
    );
  }
}
