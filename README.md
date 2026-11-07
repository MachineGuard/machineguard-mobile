# MachineGuard Mobile

Aplicación móvil de MachineGuard (Flutter, Material 3, Android). Permite consultar el estado de las zonas, revisar las alertas y atender los incidentes desde el celular. Consume el [Core API](https://github.com/MachineGuard/machineguard-core-api).

El diseño visual sigue el Design System del Capítulo V del informe: misma paleta que la Web Application, navegación inferior con tres destinos (Dashboard, Alerts, Settings) y modo oscuro.

## Ejecutar

Requisitos: Flutter 3.44 y un emulador o dispositivo Android.

```sh
flutter pub get
flutter run --dart-define=API_BASE_URL=http://10.0.2.2:8080
```

`10.0.2.2` es el `localhost` del equipo visto desde el emulador de Android. Para el Core API local, usar el `docker compose` descrito en su README. En un dispositivo físico, usar la IP del equipo en la red local.

```sh
flutter analyze
flutter test
```

## Arquitectura

Se organiza por Bounded Context, igual que la Web Application.

```text
lib/
  core/                       Configuración (API_BASE_URL) y tema
  shared/widgets/             Componentes reutilizables
  bounded_contexts/
    iam/                      Sesión, login y perfil
    environmental_monitoring/ Zonas y lecturas actuales
    alert_incident_management/ Alertas e incidentes
      domain/                 Modelos e interfaces de repositorio
      application/            Estado de la pantalla (Provider)
      infrastructure/         Implementación contra el Core API
      presentation/           Páginas y componentes
```

Cada Bounded Context define su interfaz de repositorio en `domain/`. Mientras la capa de datos real no esté lista, las pantallas se desarrollan contra una implementación de prueba de esa interfaz, y después se cambia la implementación sin tocar las páginas.

## Flujo de trabajo

GitFlow: una rama por funcionalidad desde `develop`, y merge de vuelta a `develop` por Pull Request. `main` solo recibe merges de `develop` en las entregas.

- Ramas: `feat/<tema>`, `fix/<tema>`, `docs/<tema>`.
- Commits con Conventional Commits: `feat(alerts): add severity filter chips`.
- Todo PR debe pasar `flutter analyze` y `flutter test` (el CI lo comprueba).

El reparto de tareas del Sprint 2 está en [docs/tareas-sprint2.md](docs/tareas-sprint2.md).
