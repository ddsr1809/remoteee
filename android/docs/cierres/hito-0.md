## Cierre del Hito 0: Preparación

Estado: completo
Criterios de aceptación:
- ✅ Cada versión de la tabla tiene una fuente oficial — `docs/VERSIONES.md` enlaza la página oficial de cada pieza (3 marcadas "verificar" porque se vieron a través de un espejo).
- ✅ `PROGRESO.md` contiene las tareas de los hitos 0 a 7.
- ✅ No hay ningún secreto en el repositorio — `.gitignore` excluye `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`, `admob.properties`, etc.

Qué cambió: repositorio con `README.md`, `.gitignore`, `PROGRESO.md` y `docs/VERSIONES.md`.
Cómo compilar y probar: no aplica (no hay código todavía).
Pruebas manuales que debo hacer yo: ninguna.
Decisiones que tomé: D-01 a D-06 en `PROGRESO.md` (minSdk 26, ktlint, compileSdk 37 / targetSdk 36, AGP 9 con Kotlin integrado, Billing 9.1, SDK de anuncios a decidir en H6).
Pendientes y riesgos: el nombre "Remoto" y el paquete `app.remoto.control` son suposiciones mías.
Preguntas para mí: ver el cierre del hito 1.
