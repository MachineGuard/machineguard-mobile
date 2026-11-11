# Revisión conjunta: dashboard y alertas (10/11/2026)

Se revisó la rama `feature/dashboard-zones` conservando todos los cambios locales. No se encontraron
`AGENTS.md` aplicables. No se creó ni cambió de rama, ni se hizo commit, push o merge.

## Resultado para el equipo

Terminadas las UI de dashboard-zones y alerts-list, sus estados principales, componentes, filtros
de alertas y previews debug. `MainActivity` carga `AppShell`; Dashboard es el destino inicial y
Alerts se abre desde la barra inferior. Ambos siguen en el `when` de navegación junto con Settings.
Esta conexión se verificó por código; la navegación pulsando botones aún requiere comprobación manual.

Producción invoca `DashboardUiState.Unavailable` y `AlertsUiState.Unavailable`. Sus mensajes indican
que la consulta aún no está disponible en esta versión. No afirman que no existan zonas o alertas,
ni que se haya consultado Core, ni ofrecen un reintento sin integración. Las fixtures y previews
solo están en `app/src/debug/java`; no hay referencias a ellas en `app/src/main` ni un repositorio
simulado en el flujo de la app. `compileReleaseKotlin` también pasó, sin depender del source set debug.

No se detectó una corrección de código necesaria en la navegación o los filtros. Se aclaró en
`alerts-list.md` que Activas es una agrupación de UI y que filtrar una página aislada sería insuficiente.
La UI está preparada para recibir estado; no está integrada con datos reales.

## Filtros contrastados con Core

| Filtro de UI | Estados de alerta incluidos |
|---|---|
| Activas | `PENDING`, `ESCALATED` |
| Reconocidas | `ACKNOWLEDGED` |
| Todas | `PENDING`, `ACKNOWLEDGED`, `ESCALATED`, `CLOSED` |

Core define esos cuatro estados en `AlertStatus.java`. `AlertController.list` admite un único
`status` opcional y `monitoringZoneId`; no hay valores `ACTIVE` o `ALL`, ni parámetro de estados
múltiples. La agrupación Activas es una convención de presentación consistente con Web, no una
categoría definida por Core. Debe confirmarse con el equipo al integrar. La UI explica que incluye
pendientes y escaladas y lo comprueba en pruebas unitarias.

Se necesita el listado completo **sin filtrar por estado en origen**. Se conserva el orden de
Core: `raised_at DESC, id ASC`, confirmado en `AlertJpaRepository`. El contrato actual no pagina.
Si Diego incorpora paginación o entrega un subconjunto, hay que rediseñar la consulta/filtros para
que un resultado vacío local no se presente como ausencia global. Todas incluye cerradas dentro del
ámbito autorizado devuelto por el repositorio. No se mezclan estados de Alert con los de Incident.

## Lo que necesita Camilla de Diego

Definir las interfaces de repositorio en `domain` y sus modelos reales; las proyecciones actuales
de `presentation` no sustituyen esos contratos. Las firmas deben permitir consulta y recarga,
éxito/vacío/error y cancelación; acordar errores de sesión, datos en caché y valores desconocidos.
Después se incorporarán los ViewModels en `application` y el mapeo hacia los estados ya existentes.

| Funcionalidad | Datos requeridos |
|---|---|
| Dashboard | Zona: identidad, nombre, descripción, condición ambiental distinta de ACTIVE/INACTIVE, contadores online/offline/inactivos, última actualización; puntos con identidad/nombre y últimas mediciones por punto/variable, valor decimal y fecha |
| Alertas | Lista completa ordenada; `id` y `incidentId` separados, `message`, `severity` (`LOW/MEDIUM/HIGH/CRITICAL`), `status` y `raisedAt`, `acknowledgedAt`, `escalatedAt` |

Fuentes concretas: `EnvironmentalMonitoringResources.ZoneResource` y `MeasurementResource` para
zonas, y `AlertResources.AlertResource` para alertas, dentro del Core API del workspace. Las tablas
de mapeo están en [dashboard-zones.md](dashboard-zones.md) y [alerts-list.md](alerts-list.md).

No hay destino móvil de detalle de incidentes en esta rama. Jhoan debe publicar su destino y contrato
de navegación. Las tarjetas de alertas no son pulsables; `id` de alerta nunca sustituye a `incidentId`.
No se añadieron clientes HTTP, tokens, refresh, caché ni acciones de reconocimiento/resolución.

## Verificación realizada y límite visual

Comando ejecutado:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug compileReleaseKotlin '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NONE'
```

Resultado: `BUILD SUCCESSFUL`; suite existente de nueve pruebas aprobadas (cinco de alertas, tres de
dashboard, una de destinos), tareas debug reutilizadas sin cambios de código. Lint: cero errores y
trece advertencias preexistentes. También pasó `git diff --check`. Compilar release prueba la
independencia de las fixtures debug; no significa que se haya generado o ejecutado un APK release.

Se inició el AVD disponible `Pixel_3`, en modo headless y `-read-only`, sin guardar snapshot. El APK
debug se instaló (`Success`) y se lanzó `MainActivity` (`am start -W`, `Status: ok`). Sin embargo,
Android registró muertes repetidas de procesos, incluido MachineGuard: `lowmemorykiller`,
`reason: min watermark is breached even after kill`, salida por señal 9. Las capturas no permitieron
validar las pantallas y UI Automator devolvió una raíz nula. Se cerró únicamente el emulador iniciado
para esta revisión. No se cambió la configuración persistente del AVD ni se borraron sus datos.

**No se acredita una ejecución estable, revisión visual del dashboard/lista, interacción con los
chips, ni validación de TalkBack.** Android Studio no se controló desde estas herramientas. El
intento de emulador no sustituye la revisión manual siguiente. No se atribuye ese cierre a un fallo
de la UI sin evidencia: el registro muestra intervención del gestor de memoria del emulador.

## Pasos exactos para revisión manual

1. Abrir `C:\Prueba\iot\machineguard-mobile` en Android Studio y esperar Gradle Sync. En
   **Build Variants**, elegir `debug` para el módulo `app`.
2. Abrir `app/src/debug/java/com/machineguard/mobile/boundedcontexts/environmentalmonitoring/presentation/DashboardPreviews.kt`.
   Seleccionar **Split** o **Design**, y **Build & Refresh** en Preview. Comprobar las variantes
   claro, oscuro y texto grande para carga, vacío, error, indisponibilidad y contenido. Revisar las
   previews individuales de zona para las cinco condiciones, fechas, valores ausentes y texto largo.
3. Abrir `app/src/debug/java/com/machineguard/mobile/boundedcontexts/alertincidentmanagement/presentation/AlertsPreviews.kt`
   y repetir **Build & Refresh**. Revisar todos los estados, incluido contenido sin coincidencias.
   Las previews de tarjeta cubren las cuatro severidades y estados, fechas ausentes y mensaje largo.
4. Activar **Interactive Mode** en la preview de alertas con contenido. Activas debe mostrar dos
   ejemplos (pendiente y escalada); Reconocidas uno; Todas cuatro, incluida cerrada. En la preview con
   solo una alerta cerrada, Activas debe mostrar vacío filtrado y Todas recuperar esa tarjeta.
5. En cada archivo, activar la preview interactiva de reintento. Pulsar **Reintentar**: cambia del
   ejemplo Error al ejemplo Content. Es un evento visual debug, sin consulta de red.
6. Para ejecutar la app, abrir **Device Manager**, iniciar un AVD con memoria disponible suficiente
   o conectar un dispositivo Android API 26+, seleccionarlo y pulsar **Run app**. Si se repiten
   cierres por memoria, cerrar emuladores innecesarios o usar un dispositivo físico; el arranque no
   demuestra que la UI llegó a renderizarse.
7. En la app comprobar **Dashboard → Alerts → Settings → Dashboard**. Dashboard debe mostrar
   “Monitoreo no disponible” y Alerts “Alertas no disponibles”, con explicación y sin tarjetas de
   ejemplo, chips ni botones ficticios. Esos son los estados de producción esperados hoy.
8. Revisar tamaños de fuente grandes, modo oscuro y TalkBack en el dispositivo. Las tarjetas con
   datos y filtros se revisan en previews hasta integrar el repositorio; no cambiar producción a
   fixtures para obtener capturas.

No hay integración con Core verificada desde Mobile. Esta revisión conjunta solo modificó
documentación; preserva la implementación y el trabajo previo de ambas funcionalidades.
