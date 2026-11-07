import 'package:flutter_test/flutter_test.dart';
import 'package:machineguard_mobile/app.dart';

void main() {
  testWidgets('shows the three bottom navigation destinations', (tester) async {
    await tester.pumpWidget(const MachineGuardApp());

    expect(find.text('Dashboard'), findsWidgets);
    expect(find.text('Alerts'), findsOneWidget);
    expect(find.text('Settings'), findsOneWidget);
  });

  testWidgets('switches to the alerts destination', (tester) async {
    await tester.pumpWidget(const MachineGuardApp());

    await tester.tap(find.text('Alerts'));
    await tester.pumpAndSettle();

    expect(find.text('Alertas activas y reconocidas, con filtros rápidos.'),
        findsOneWidget);
  });
}
