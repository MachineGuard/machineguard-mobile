# MachineGuard Mobile

Aplicación móvil de MachineGuard para Android, escrita en Kotlin con Jetpack Compose y Material 3. Permite consultar el estado de las zonas, revisar las alertas y atender los incidentes desde el celular. Consume el [Core API](https://github.com/MachineGuard/machineguard-core-api).

El diseño visual sigue el Design System del Capítulo V del informe: la misma paleta que la Web Application, navegación inferior con tres destinos (Dashboard, Alerts, Settings) y modo oscuro.

## Requisitos

- Android Studio (incluye el JDK que usa Gradle).
- Un emulador o dispositivo con Android 8.0 (API 26) o superior.
- El Core API en ejecución. Ver su README para levantarlo con Docker Compose.

## Ejecutar

Abrir la carpeta del repo en Android Studio y ejecutar la configuración `app`. Por línea de comandos:

```sh
./gradlew installDebug
```

La app apunta por defecto a `http://10.0.2.2:8080`, que es el `localhost` del equipo visto desde el emulador. Para otra URL, por ejemplo un dispositivo físico en la red local:

```sh
./gradlew installDebug -PapiBaseUrl=http://192.168.1.10:8080
```

Pruebas y APK de depuración:

```sh
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/`.

## Arquitectura

Se organiza por Bounded Context, igual que la Web Application.

```text
app/src/main/java/com/machineguard/mobile/
  core/                        Configuración (URL del API) y tema
  shared/ui/                   Componentes reutilizables
  boundedcontexts/
    iam/                       Sesión, login y perfil
    environmentalmonitoring/   Zonas y lecturas actuales
    alertincidentmanagement/   Alertas e incidentes
      domain/                  Modelos e interfaces de repositorio
      application/             Estado de la pantalla (ViewModel)
      infrastructure/          Implementación contra el Core API
      presentation/            Pantallas (Compose)
```

Cada Bounded Context define su interfaz de repositorio en `domain`. Mientras la capa de datos real no esté lista, las pantallas se desarrollan contra una implementación de prueba de esa interfaz, y después se cambia la implementación sin tocar las pantallas.

Dependencias ya incluidas para la capa de datos: OkHttp, kotlinx.serialization y `androidx.security:security-crypto` para guardar el token.

## Flujo de trabajo

GitFlow: una rama por funcionalidad desde `develop`, y merge de vuelta a `develop` por Pull Request. `main` solo recibe merges de `develop` en las entregas.

- Ramas: `feat/<tema>`, `fix/<tema>`, `docs/<tema>`.
- Commits con Conventional Commits: `feat(alerts): add severity filter chips`.
- Todo PR debe pasar `./gradlew testDebugUnitTest assembleDebug` (el CI lo comprueba y publica el APK como artefacto).

El reparto de tareas del Sprint 2 está en [docs/tareas-sprint2.md](docs/tareas-sprint2.md).
