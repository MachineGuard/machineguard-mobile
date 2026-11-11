# Lista de alertas: primera entrega

La [revisión conjunta de dashboard y alertas](revision-dashboard-alerts.md) contiene el estado más
reciente de verificaciones, el intento de ejecución en emulador y los pasos manuales pendientes.

## Alcance

Implementación en `feature/dashboard-zones`, por instrucción de Camilla. Se conservan los cambios
locales de dashboard; no se crea otra rama ni se modifica su implementación. Se leyeron
`docs/tareas-sprint2.md` y `docs/dashboard-zones.md`; no se encontraron `AGENTS.md` aplicables.

`AlertsScreen()` sustituye el placeholder usando la entrada existente de `AppShell`. Dashboard,
Alerts y Settings siguen disponibles. No se modificaron navegación, login ni pantallas de compañeros.

`AlertsContent(state, onRetry)` implementa carga, contenido, vacío, error con reintento e
indisponibilidad. El filtro local se conserva con `rememberSaveable`. En producción se muestra
**Alertas no disponibles**, sin fixtures, peticiones, chips inutilizables ni botón de reintento.
El callback de reintento solo se presenta en el estado Error y debe conectarse a una recarga real
cuando llegue el repositorio. No hay ViewModel ni repositorio provisional.

Las tarjetas informativas muestran mensaje, severidad, estado, fecha de generación y, cuando existen,
fechas de reconocimiento y escalamiento. No incluyen zona ni mediciones: `AlertResource` no aporta
esos campos. Las fechas se muestran en la zona horaria del dispositivo; una fecha ausente no se
inventa. La severidad tiene etiqueta textual además del color. Encabezados y regiones de anuncio
cortés permiten entender estados y resultados con TalkBack. Los chips Material comunican selección
y se distribuyen en varias líneas si hace falta. Todos los textos de la nueva UI usan los recursos
existentes en `app/src/main/res/values/strings.xml`.

## Contratos consultados y filtros

Referencias locales de solo lectura:

- Core: `docs/alerts.md` y, bajo `src/main/java/com/machineguard/platform/alertincidentmanagement/`,
  `interfaces/rest/resources/AlertResources.java`, `interfaces/rest/AlertController.java`,
  `domain/model/AlertStatus.java`, `domain/model/AlertSeverity.java` e
  `infrastructure/persistence/repository/AlertJpaRepository.java`.
- Web: `src/app/dashboard/services/dashboard.facade.ts` y
  `src/app/bounded-contexts/alert-incident-management/presentation/components/latest-alerts/latest-alerts.component.ts`.
  Se consulta su agrupación de estados; sus severidades antiguas no reemplazan las de Core.

Core documenta `GET /api/v1/alerts`, con array sin paginación, parámetros opcionales
`monitoringZoneId` y `status`, orden `raised_at DESC, id ASC`. La UI conserva el orden recibido;
no ordena por severidad ni aplica límites. Los filtros exigidos por el sprint se traducen así:

| Chip | Estados incluidos |
|---|---|
| Active / Activas | `PENDING`, `ESCALATED`, siguiendo la agrupación de alertas pendientes de Web |
| Acknowledged / Reconocidas | `ACKNOWLEDGED` |
| All / Todas | Todos, incluido `CLOSED` |

`ACTIVE` es un filtro de presentación, no un valor de `AlertStatus` ni un parámetro admitido por
Core. Se aplica sobre el listado completo sin filtrar por estado en origen; no se deben consultar
solo `PENDING` y luego intentar mostrar reconocidas. El filtro inicial es Activas, con una explicación
visible de los estados incluidos. Se distingue el array vacío de un filtro sin coincidencias.

Limitación confirmada en la revisión conjunta: Core **no define una categoría Activas**. La unión
`PENDING` + `ESCALATED` es una convención de presentación respaldada por los estados de Core y la
agrupación de Web, no un alias oficial del backend. El parámetro `status` admite un único valor;
no acepta `ACTIVE`, `ALL` ni una lista de estados. Para estos chips, Diego debe entregar el array
completo sin `status`; Todas significa todas las alertas autorizadas devueltas por esa consulta.
Si en el futuro se añade paginación, habrá que revisar el filtrado: una página aislada no permite
afirmar que no hay coincidencias en el conjunto completo. No se deducen estados de Incident.

## Pendiente con Diego S. y Jhoan

No existen interfaces ni modelos de alertas de la capa de datos móvil en esta rama.
`AlertsUiState`, `AlertCardUiModel`, `AlertSeverity` y `AlertStatus` son proyecciones de presentación,
no DTOs ni contratos alternativos de dominio. Diego debe definir el repositorio en `domain`:

- Consulta/recarga del listado completo para la organización autenticada, conservando el orden de Core.
- Resultado de éxito/error y representación de cancelación y errores de sesión, sin mensajes técnicos
  en pantalla. Autenticación, HTTP, tokens, refresh y caché siguen siendo responsabilidad de datos/IAM.
- Modelo con los campos de Core y política de fechas y valores desconocidos. Las proyecciones permiten
  fecha o relación ausentes de forma defensiva; Core define `incidentId` y `raisedAt` en el recurso.
  No se deben convertir futuros estados/severidades desconocidos en PENDING o LOW silenciosamente;
  acordar su tratamiento antes de conectar el adaptador.

| Campo de `AlertResource` | Presentación |
|---|---|
| `id` | `AlertCardUiModel.id`, clave estable de la alerta |
| `incidentId` | Relación separada, reservada para la futura navegación |
| `severity` | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL` |
| `status` | `PENDING`, `ACKNOWLEDGED`, `ESCALATED`, `CLOSED` |
| `message` | Mensaje de la tarjeta |
| `raisedAt`, `acknowledgedAt`, `escalatedAt` | Fechas de generación y eventos disponibles |

Después se agregará un ViewModel en `application`, el adaptador de los modelos de Diego y la carga
real. La UI exige la lista completa para aplicar los tres filtros localmente.

**Alert e Incident son entidades distintas.** `GET /api/v1/alerts/{alertId}` recibe una identidad de
alerta; no es un destino móvil ni un detalle de incidente. No existe destino de incidentes en la
navegación de esta rama. Las tarjetas no son pulsables, no tienen botones de detalle y nunca envían
`id` como si fuera `incidentId`. Cuando Jhoan publique su destino se acordará la ruta y se habilitará
solo para relaciones válidas mediante `incidentId`. No se implementan reconocimiento, resolución,
push, escalamiento ni generación de alertas. Core también documenta límites de su entrega de consultas;
esta UI no acredita que exista un flujo real de generación de alertas.

## Archivos

- `app/src/main/java/com/machineguard/mobile/boundedcontexts/alertincidentmanagement/presentation/`:
  `AlertsScreen.kt`, `AlertsUiState.kt` y `AlertCard.kt`.
- `app/src/main/res/values/strings.xml`: textos de la funcionalidad.
- `app/src/debug/java/com/machineguard/mobile/boundedcontexts/alertincidentmanagement/presentation/AlertsPreviews.kt`.
- `app/src/test/java/com/machineguard/mobile/boundedcontexts/alertincidentmanagement/presentation/AlertsUiStateTest.kt`.

## Cómo probar

En Android Studio, seleccionar la variante debug y abrir `AlertsPreviews.kt`. Incluye carga, vacío,
error, indisponibilidad, contenido y filtro sin resultados, en claro, oscuro y texto grande; tarjetas
individuales cubren todas las severidades y estados, mensajes largos y valores ausentes. Los ejemplos
solo pertenecen al source set debug. En modo interactivo, cambiar chips y verificar que Todas incluye
cerradas, Reconocidas solo reconocidas y Activas pendientes/escaladas. El reintento interactivo cambia
el estado visual de ejemplo; no realiza una petición real.

Desde PowerShell, en la raíz de Mobile:

```powershell
$env:ANDROID_HOME = "$env:LOCALAPPDATA\Android\Sdk"
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug
.\gradlew.bat installDebug
```

En este equipo, si se requiere el almacén de certificados de Windows para Gradle:

```powershell
.\gradlew.bat testDebugUnitTest assembleDebug lintDebug '-Djavax.net.ssl.trustStoreType=Windows-ROOT' '-Djavax.net.ssl.trustStore=NONE'
```

Compilar genera `app/build/outputs/apk/debug/app-debug.apk`; instalar requiere un emulador o dispositivo
conectado. Abrir la app con Run en Android Studio y cambiar entre Dashboard, Alerts y Settings.
En Alerts es correcto ver indisponibilidad hasta integrar datos. Comprobar TalkBack, texto grande,
scroll y filtros en previews interactivas; una compilación correcta no demuestra revisión visual.

## Verificación realizada (10/11/2026)

- `testDebugUnitTest assembleDebug lintDebug`: `BUILD SUCCESSFUL`.
- Nueve pruebas aprobadas: cinco de alertas, tres de dashboard y una de destinos de navegación.
  Las nuevas cubren filtros, vacío global y filtrado, preservación del orden y fechas, separación de
  identidad alerta/incidente y copia del listado recibido sin depender de mutaciones posteriores.
- APK debug generado. Lint: cero errores y trece advertencias de versiones de dependencias y
  configuración preexistente; no se actualizaron dependencias como parte de esta tarea.
- `git diff --check` pasó. Se mantiene `feature/dashboard-zones` y los cambios previos del dashboard.
- No se instaló ni ejecutó en dispositivo/emulador, no se inspeccionó el render de las previews y no
  se realizó una prueba con TalkBack. Esas comprobaciones manuales siguen pendientes.
- No hay integración real con Core desde Mobile en esta entrega. No se hizo commit, push ni merge.
