class AppConfig {
  /// URL base del Core API. En el emulador de Android, 10.0.2.2 apunta al
  /// localhost del equipo. Se sobrescribe con --dart-define=API_BASE_URL=...
  static const apiBaseUrl = String.fromEnvironment(
    'API_BASE_URL',
    defaultValue: 'http://10.0.2.2:8080',
  );
}
