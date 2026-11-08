# Tareas de Mobile: Sprint 2 (AV2)

Alcance mínimo: login, zonas con lecturas actuales, lista de alertas con filtros y atención de incidentes. Fecha límite de entrega: **martes 10/11**.

Cada tarea va en su propia rama desde `develop` y se integra por Pull Request.

| Responsable | Rama | Tarea | Endpoints del Core API |
|---|---|---|---|
| Karito Medina | `feat/iam-login` | Pantalla de login, navegación según la sesión y pantalla Settings con cierre de sesión | `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/users/me` |
| Diego Bautista | `feat/data-layer` | Cliente HTTP con OkHttp y kotlinx.serialization, guardado seguro del token, renovación de sesión, modelo de errores y caché de la última lectura | `POST /api/v1/auth/refresh` y el resto como base |
| Camilla Espinoza | `feat/dashboard-zones` | Dashboard con tarjetas de zona ordenadas por estado y lecturas actuales | `GET /zones`, `GET /measurements/latest` |
| Camilla Espinoza | `feat/alerts-list` | Lista de alertas con chips Active, Acknowledged y All, indicador de severidad y hora | `GET /api/v1/alerts` |
| Jhoan Janampa | `feat/incident-detail` | Detalle de alerta y atención del incidente (reconocer y resolver) | `GET /api/v1/alerts/{alertId}` y los endpoints de incidentes de Alert & Incident Management |
| Sandro Dinklange | `test/mobile-suite` | Pruebas unitarias y de interfaz, y revisión del CI | No aplica |
| Pedro Lecca | No aplica | Confirmar los endpoints, usuario y datos de demostración | Todos |

## Orden de trabajo

1. **Dom 8/11:** cada integrante desarrolla su pantalla contra repositorios de prueba. Diego Bautista fija primero las interfaces de repositorio en `domain`, para que nadie dependa de él.
2. **Lun 9/11:** se reemplazan los repositorios de prueba por los reales y se prueba contra el Core API local.
3. **Mar 10/11:** APK generado, capturas y video de navegación como evidencia del Sprint 2, y merge a `develop`.

## Evidencia para el informe (Capítulo VI)

Cada integrante documenta su aporte en la sección 6.2.2 del informe, con commits, pruebas y capturas. El Capítulo V todavía describe Mobile como aplazada y debe actualizarse.
