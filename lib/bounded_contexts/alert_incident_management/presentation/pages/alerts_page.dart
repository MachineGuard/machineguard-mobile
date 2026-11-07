import 'package:flutter/material.dart';

import '../../../../shared/widgets/feature_placeholder.dart';

class AlertsPage extends StatelessWidget {
  const AlertsPage({super.key});

  @override
  Widget build(BuildContext context) {
    return const FeaturePlaceholder(
      title: 'Alerts',
      description: 'Alertas activas y reconocidas, con filtros rápidos.',
      icon: Icons.notifications_outlined,
    );
  }
}
