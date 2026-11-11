# Capa de datos de Mobile

Define cómo la app habla con el [Core API](https://github.com/MachineGuard/machineguard-core-api). Las pantallas dependen
solo de las **interfaces de repositorio** de `domain`; la implementación real (OkHttp, token seguro, renovación de sesión,
errores y caché) se puede cambiar sin tocarlas.

## Cómo usar un repositorio desde una pantalla

Cada función es `suspend` y devuelve un `DataResult<T>`; **nunca lanza excepciones de red**:

```kotlin
when (val result = zones.getZones()) {
    is DataResult.Success -> {
        show(result.data)
        if (result.isFromCache) showBanner("Última actualización: ${result.fetchedAt}")
    }
    is DataResult.Failure -> when (val error = result.error) {
        AppError.SessionExpired -> Unit            // la capa de datos ya cerró la sesión: navegar según AuthRepository.session
        else -> showError(error, retry = error.isRetryable)
    }
}
```

- `Success.source` es `CACHE` cuando no hubo conexión y se muestra la última copia guardada.
- `Failure.error` es un `AppError` (`Offline`, `Timeout`, `SessionExpired`, `InvalidCredentials`, `Forbidden`, `NotFound`,
  `Conflict`, `Invalid`, `TooManyRequests`, `Server`, `Unknown`). No hay que mostrar `error.detail` tal cual: es el texto
  del servidor y solo sirve como apoyo (por ejemplo, el motivo de un `Conflict`).
- Las pantallas **no** manejan tokens ni renuevan sesión: si la sesión termina, `AuthRepository.session` pasa a
  `SignedOut(expired = true)` y la navegación vuelve al login.

## Interfaces por Bounded Context

| Contexto | Interfaz | Funciones | Endpoints del Core API |
|---|---|---|---|
| IAM | `AuthRepository` | `session`, `login`, `logout`, `refreshProfile` | `POST /auth/login`, `POST /auth/logout`, `GET /users/me` |
| Environmental Monitoring | `ZonesRepository` | `getZones`, `getZone` | `GET /environmental-monitoring/zones[/{id}]` |
| Environmental Monitoring | `MeasurementsRepository` | `getLatest(zoneId?)` | `GET /environmental-monitoring/measurements/latest` |
| Alertas e Incidentes | `AlertsRepository` | `getAlerts(query)`, `getAlert(id)` | `GET /alerts`, `GET /alerts/{id}` |
| Alertas e Incidentes | `IncidentsRepository` | `getIncidents`, `getIncident`, `acknowledgeAlert`, `registerCorrectiveAction`, `resolveIncident` | `GET /incidents[/{id}]`, `POST /alerts/{id}/acknowledgements`, `POST /incidents/{id}/corrective-actions`, `POST /incidents/{id}/resolve` |

Todas las rutas empiezan por `/api/v1`. Los modelos están en el paquete `domain` de cada contexto.

## Decisiones de representación

- **Identificadores:** `String` (UUID en texto).
- **Fechas:** `OffsetDateTime` (el servidor envía ISO 8601). Nulo cuando el dato no existe.
- **Lecturas:** `BigDecimal` exacto, nunca `Double`; una variable sin lectura no aparece en la lista (no se rellena con cero).
- **Estados desconocidos:** todos los enums de estado tienen un valor `UNKNOWN`. Si el servidor agrega un estado nuevo la
  app no se cae; la pantalla debería tratar `ZoneCondition.UNKNOWN` como `NO_DATA`.
- **Condición de zona:** `ZoneCondition` es la condición ambiental (`OUT_OF_RANGE`, `NEAR_LIMIT`, `OFFLINE`, `NO_DATA`,
  `NORMAL`), distinta del estado administrativo `ZoneStatus`.
- **Nombre de un punto:** `Zone.pointName(measurement.pointId)`.
- **Resolver un incidente:** usar `IncidentDetail.resolutionAllowed` para habilitar el botón; `resolutionBlockReasons`
  explica por qué no.
- **Autoría:** la app nunca envía quién reconoce, registra o resuelve; el servidor usa el usuario del token.
- **Escrituras:** no usan caché ni se reintentan solas; sin conexión devuelven `Failure(Offline)`.

Los modelos de presentación (`ZoneCardUiModel`, `AlertCardUiModel`, ...) siguen siendo independientes: cada pantalla
adapta los modelos de `domain` a sus `UiModel`.

## Obtener los repositorios reales

`MachineGuardApplication` crea un `AppContainer` al iniciar la app. Desde cualquier `Context` (actividad, composable,
`AndroidViewModel`):

```kotlin
val container = context.appContainer
container.authRepository        // AuthRepository
container.zonesRepository       // ZonesRepository
container.measurementsRepository
container.alertsRepository      // AlertsRepository
container.incidentsRepository   // IncidentsRepository
```

Siempre se usan como la interfaz de `domain`, nunca la clase `...Impl`. Para probar una pantalla sin red, basta crear una
implementación falsa de la interfaz y pasarla al ViewModel.

### Sesión (para Karito)

- Al arrancar, `session` vale `Restoring` hasta que se lee el almacenamiento seguro (milisegundos). Mostrar una pantalla
  de carga y luego decidir: `SignedIn` abre la app, `SignedOut` abre el login.
- `SignedOut(expired = true)` aparece solo, cuando una renovación de sesión falla en cualquier petición: la navegación
  debe volver al login y puede explicar "Tu sesión terminó".
- `logout()` borra siempre la sesión local y la caché, aunque no haya red.
- `login` y `refreshProfile` devuelven el `UserProfile`; `UserProfile.canManageIncidents` indica si el usuario puede
  reconocer, registrar acciones y resolver (solo `ADMIN`).

## Cómo funciona por dentro

```text
core/
  data/      DataResult, AppError, RemoteSource (red + caché)
  network/   ApiClient (peticiones y errores), AuthInterceptor, TokenAuthenticator, HttpClientFactory, ApiJson
  session/   SessionManager (estado y renovación), EncryptedSessionStorage (Keystore)
  cache/     FileLocalCache
  di/        AppContainer
boundedcontexts/<contexto>/infrastructure/   DTOs del API, mapeos a domain y los repositorios
```

- **Cliente HTTP:** OkHttp con `kotlinx.serialization`. Tiempos de espera: conexión 10 s, lectura 20 s, total 30 s. No
  hay reintentos automáticos de conexión (una escritura no debe duplicarse) y no se registra el contenido de las
  peticiones, porque llevan tokens y contraseñas.
- **Token seguro:** access y refresh token y el perfil se guardan en `EncryptedSharedPreferences` (AES-256, llave en el
  Keystore). Si el Keystore ya no puede descifrarlos, se descarta la sesión y se pide el login, en lugar de fallar.
- **Renovación de sesión:** ante un 401, `TokenAuthenticator` renueva con `POST /auth/refresh` y reintenta la petición
  una vez. Las renovaciones están serializadas: el servidor rota el refresh token en cada uso y revoca la sesión si
  ve reutilizar uno viejo, así que varias peticiones que fallen a la vez renuevan una sola vez. Si el servidor rechaza
  el refresh token (400, 401 o 403), la sesión termina; si falla por red, 5xx o 429, la sesión se conserva y la petición
  se informa como `Offline`.
- **Errores:** ver la tabla en `AppError`. El login responde `InvalidCredentials` ante un 401.
- **Caché:** la última respuesta correcta de cada consulta se guarda como JSON en `noBackupFilesDir/api-cache`. Solo se
  usa si la red falla (sin conexión, tiempo agotado o 5xx); un 401, 403, 404 o 409 nunca se tapa con la caché. Una
  escritura correcta invalida las cachés de alertas e incidentes. Se borra al cerrar o perder la sesión.

## Pruebas

`./gradlew testDebugUnitTest` incluye 59 pruebas de la capa de datos contra un servidor simulado (MockWebServer): mapeo
de errores, renovación de sesión (incluida la concurrencia y el rechazo del refresh token), caché y recuperación sin
conexión, mapeo de modelos con valores desconocidos, decimales exactos, y las cinco operaciones de incidentes.

No se probó todavía contra un Core API en ejecución ni en un emulador: los JSON de las pruebas se tomaron de los recursos
del Core API (`IamResources`, `EnvironmentalMonitoringResources`, `AlertIncidentResources`).
