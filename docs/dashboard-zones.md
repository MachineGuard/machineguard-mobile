# Dashboard y zonas: primera entrega

La [revisión conjunta de dashboard y alertas](revision-dashboard-alerts.md) contiene el estado más
reciente de verificaciones, el intento de ejecución en emulador y los pasos manuales pendientes.

## Alcance disponible

`DashboardScreen()` conserva la entrada que usa `AppShell`. Mientras no exista el repositorio móvil,
la app muestra **Monitoreo no disponible**: no hace peticiones ni muestra zonas simuladas.

`DashboardContent(state, onRetry)` implementa carga, contenido, vacío y error con botón de reintento.
El callback debe conectarse a una recarga real cuando esté disponible la capa de datos; actualmente
el estado no disponible no ofrece un botón sin efecto. No se ha implementado un ViewModel provisional
ni una interfaz de repositorio alternativa.

Los archivos están en `app/src/main/java/com/machineguard/mobile/boundedcontexts/environmentalmonitoring/presentation/`.
`DashboardUiState`, `ZoneCardUiModel` y `PointReadingUiModel` son proyecciones de presentación, no DTOs.

Las tarjetas muestran nombre, descripción, condición ambiental, contadores de sensores y lecturas
separadas por punto y variable, con fecha de registro y última actualización. Los valores ausentes
no se convierten en cero. Las lecturas de una zona offline se identifican como últimas registradas.
Las fechas se presentan en la zona horaria del dispositivo.

Orden adoptado para esta entrega: fuera de rango, cerca del límite, sin conexión, sin datos y normal;
desempate por nombre e identificador. Es una decisión de presentación revisable: el sprint exige
ordenar por estado, pero no fija una prioridad. No se promedian lecturas de distintos puntos.

## Contrato pendiente con Diego S.

No hay interfaces ni modelos de dominio móviles en esta rama. Acordar antes de conectar:

- Operación de consulta/actualización de zonas y cómo expone éxito, error y carga.
- Modelos que incluyan identidad y nombre de zona, condición ambiental (distinta de ACTIVE/INACTIVE),
  contadores de sensores, última actualización, puntos y últimas lecturas por punto/variable.
- Representación de fechas, decimales, valores ausentes y estados desconocidos.
- Cómo identificar datos procedentes de caché y errores de sesión, sin mostrar errores técnicos.

La referencia real es `EnvironmentalMonitoringResources.java` del Core API, bajo
`src/main/java/com/machineguard/platform/environmentalmonitoring/interfaces/rest/resources/`.
`ZoneResource` ya contiene `latestMeasurements` y `points`. El futuro mapeo debe enlazar las lecturas
con sus puntos y conservar las condiciones NO_DATA y OFFLINE por separado.
Diego define el repositorio y su implementación. Después se incorpora un ViewModel en `application`
que exponga el estado y ejecute el reintento. No se necesitan cambios de endpoints para esta UI.

Karito conserva la responsabilidad de sesión y login. No se modificó `AppShell` ni Settings.
Esta entrega no añade detalle de zona, acciones de incidentes ni cambios en Alerts; no depende del
detalle de Jhoan para mostrar tarjetas.

## Revisión visual y comprobaciones

En Android Studio seleccionar la variante debug y abrir
`app/src/debug/java/com/machineguard/mobile/boundedcontexts/environmentalmonitoring/presentation/DashboardPreviews.kt`.
Incluye todos los estados en claro, oscuro y texto grande, además de un reintento interactivo de ejemplo.
Las fixtures solo pertenecen al source set debug y ninguna entrada de producción las referencia.
También hay previews individuales de `ZoneCard` para las cinco condiciones, en claro, oscuro y
texto grande; permiten revisar todas las tarjetas sin depender del scroll de la preview completa.
El título del dashboard y los nombres de zona se identifican como encabezados para TalkBack.
Los mensajes de estado usan una región de anuncio cortés al cambiar el estado de la pantalla.

Desde PowerShell en la raíz de Mobile:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat installDebug
```

Si Java informa `PKIX path building failed` al descargar dependencias en este equipo Windows,
se puede usar su almacén de certificados para esa ejecución, manteniendo la validación TLS:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NONE'
```

La instalación requiere dispositivo o emulador. En la app verificar Dashboard y el cambio entre
los tres destinos; es correcto ver el mensaje de monitoreo no disponible hasta integrar datos.
En las previews revisar scroll, texto grande, condiciones, ausencia de lecturas y botón de reintento.
Las pruebas unitarias cubren vacío, orden y preservación de lecturas por punto.

## Punto de reanudación verificado (10/11/2026)

Se conservó la rama `feature/dashboard-zones` y todo el trabajo local existente. No se encontraron
archivos `AGENTS.md` aplicables. El alcance se contrastó con `docs/tareas-sprint2.md` y con los
recursos y condiciones ambientales del Core API; no se modificaron los otros repositorios.

Para el futuro adaptador de presentación, los campos confirmados son:

| Origen en Core API | Destino en presentación |
|---|---|
| `ZoneResource.id`, `name`, `description` | Identidad y texto de `ZoneCardUiModel` |
| `environmentalCondition` | `ZoneCondition`; no usar el estado administrativo `status` |
| `sensorsOnline`, `sensorsOffline`, `sensorsInactive` | Contadores de sensores |
| `lastUpdatedAt` | Última actualización de zona |
| `latestMeasurements[].monitoringPointId` y `points[].id` | Resolver el nombre del punto sin mezclar lecturas |
| `environmentalVariable`, `measuredValue`, `recordedAt` | Variable, valor decimal y fecha de `PointReadingUiModel` |

Diego debe confirmar la interfaz móvil y el tratamiento de condiciones desconocidas y puntos sin
correspondencia antes de escribir ese adaptador. Esta tabla documenta campos existentes; no define
un repositorio alternativo ni constituye una integración de red.

Verificación tras la reanudación: `testDebugUnitTest assembleDebug lintDebug` finalizó con
`BUILD SUCCESSFUL`. Pasaron cuatro pruebas (tres de dashboard y una de navegación); lint terminó
con cero errores y trece advertencias de versiones de dependencias y configuración existente.
`git diff --check` pasó. Se generó `app/build/outputs/apk/debug/app-debug.apk`.
No se ejecutó en dispositivo/emulador ni se inspeccionó el render de las previews en Android Studio.
