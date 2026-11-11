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
