## Cierre del Hito 2: Descubrimiento

Estado: completo con pendientes. El código está terminado y la lógica de búsqueda está verificada, pero **la app Android no se pudo compilar aquí** (igual que en el hito 1: el entorno no llega a Maven Central ni al Maven de Google). La primera compilación y las pruebas en tu red tienen que ser en tu equipo.

Lo que sí se verificó aquí: todo `core/discovery`, `core/net` (la parte pura), `core/diagnostics`, `RemoteSession` y los *fakes* de prueba se compilaron con Kotlin 2.2 y kotlinx-coroutines reales. Corrieron 56 pruebas puras y 16 comprobaciones del escáner, `DeviceAdder` y la fuente SSDP. Una de ellas usa **sockets UDP reales** contra un "Roku" falso que contesta en el mismo equipo: lo encuentra, lo identifica y suelta el bloqueo de multidifusión tanto al terminar como al cancelar.

Criterios de aceptación:
- ❓ (lo confirmas tú) En tu red aparecen tus dispositivos con su plataforma identificada. Prueba manual 2.
- ✅ El bloqueo de multidifusión se libera siempre, incluso si la búsqueda falla. Lo toma y lo suelta `MulticastLockProvider.withLock` en un `finally`. `MulticastLockTest` cubre el error y el arnés con sockets reales cubre la cancelación a mitad de búsqueda.
- ✅ Sin Wi-Fi, la app lo dice en lugar de buscar sin resultado. `DeviceScanner` revisa la red antes de abrir ningún socket y la prueba "sin Wi-Fi no busca y lo dice" lo comprueba. Falta confirmarlo en el teléfono (prueba manual 1).
- ✅ Pruebas del analizador SSDP (`SsdpParserTest`, 10) y de la deduplicación (`DiscoveryMergerTest`, 8).
- ✅ La pantalla del control sigue sin importar nada de `drivers/` (`grep` vacío en `ui/` y `core/`).

Qué cambió:
- **SSDP** (`SsdpDiscoverySource`): `MulticastSocket` atado a la Wi-Fi, dos rondas de `M-SEARCH` (una por plataforma) y escucha de 5 s. El `MulticastLock` se toma solo durante la búsqueda.
- **mDNS** (`MdnsDiscoverySource`): `NsdManager` con `_androidtvremote2._tcp`, resoluciones en fila, IPv4 y nombres DNS-SD legibles.
- **Lista unificada** (`DiscoveryMerger`): una tele aparece una sola vez aunque responda por las dos vías.
- **Identificación de plataforma** (`PlatformIdentifier`): por `ST`/`NT`/`USN` y después por `SERVER`.
- **Escáner** (`DeviceScanner`): primero Wi-Fi, después el permiso de red local, luego las dos vías en paralelo con un tope de 7 s. Si sale `EPERM` sin resultados, distingue "permiso negado" de "no encontré tu tele".
- **Red local** (`core/net`): detección de Wi-Fi/Ethernet sin VPN, `LocalNetworkAccess` preparada para Android 17 **sin activarla** y validación de IPv4.
- **Diagnóstico** (`core/diagnostics`): registro en memoria. Oculta los dos últimos números de cada IPv4 y también las IPv6, MAC y UUID. Se copia desde "No aparece mi tele" y desde Ajustes.
- **Pantallas**: "Buscar en mi Wi-Fi" (con estados de búsqueda, resultados, sin resultados, sin Wi-Fi, permiso y error), "No aparece mi tele" (consejos, IP manual y copiar registro) y "Agregar por IP" (validación y plataforma). La explicación del permiso de red local está escrita, pero hoy nunca se muestra.
- **Guardar** (`DeviceAdder`): no duplica teles; si ya estaba guardada, actualiza la IP y conserva el nombre.
- El control avisa con un mensaje claro si la plataforma todavía no tiene driver.
- Manifiesto: `INTERNET`, `ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE`.
- Nuevo tema de ayuda "Mi tele no aparece"; 29 textos nuevos en inglés y español (se quitó el de "próximamente" de Wi-Fi).
- `versionName` 0.2.0. Pruebas unitarias: de 39 a 107.

Cómo compilar y probar:
1. `cd android`
2. `./gradlew assembleDebug test`
3. `./gradlew installDebug` con el teléfono conectado.
4. Opcional: `./gradlew ktlintFormat ktlintCheck`.
5. Si algo no compila, pásame el primer error completo.

Pruebas manuales que debo hacer yo:
1. Con datos móviles y la Wi-Fi apagada: Inicio › "Buscar en mi Wi-Fi" → aparece "Tu teléfono no está en Wi-Fi" sin buscar. "Abrir ajustes de Wi-Fi" abre el panel de Wi-Fi; al conectarte y volver, busca sola.
2. En tu Wi-Fi con las teles encendidas: en 7 segundos o menos aparecen tus teles con su plataforma. **Anota qué aparece (nombre, plataforma, IP) y qué falta**: es la evidencia del primer criterio.
3. Si tienes Android TV / Google TV: aparece una sola vez y con el nombre que tiene en sus ajustes.
4. Toca una tele → se guarda y se abre el control con el aviso "… quedó guardada. El control para teles … llega en una próxima versión". Atrás lleva al inicio.
5. Vuelve a buscar → esa tele muestra la marca de "Ya guardada"; tocarla no crea un duplicado en "Teles guardadas".
6. "Teles guardadas" → la tele aparece con el nombre de su plataforma; se puede renombrar y borrar.
7. "¿No aparece tu tele?" → consejos. "Agregar por IP": `192.168.1` muestra error de formato, `127.0.0.1` dice que no puede ser una tele, `8.8.8.8` avisa que no parece de casa, y el botón solo se activa con una IP válida y una plataforma elegida. Agregar la misma IP y plataforma dos veces deja una sola tele.
8. "Copiar registro de diagnóstico" (en "No aparece mi tele" y en Ajustes) → pégalo en una nota: trae versión de la app, Android y modelo, y las IP salen como `192.168.x.x`.
9. Bloqueo de multidifusión: durante una búsqueda y después, ejecuta `adb shell dumpsys wifi` y busca el apartado de bloqueos de multidifusión (el nombre exacto cambia según la versión de Android). El bloqueo `remoto:ssdp` aparece solo mientras se busca.
10. Sal de la pantalla a mitad de búsqueda y vuelve a entrar: no se cierra la app y la búsqueda empieza de nuevo.
11. En una red de invitados o con aislamiento de clientes: termina en "No encontré ninguna tele" sin quedarse cargando.
12. Primera apertura (borra los datos de la app): bienvenida › "Buscar en mi Wi-Fi" → agrega una tele → al cerrar y abrir la app ya no aparece la bienvenida.
13. Con el teléfono en inglés, todos los textos nuevos cambian.
14. Con TalkBack, cada tele de la lista se anuncia con su nombre, plataforma e IP, y la acción "Agregar".
15. Las pruebas manuales del hito 1 siguen pasando (sobre todo "Probar sin tele").

Decisiones que tomé: D-11 a D-20 en `PROGRESO.md`. Las más importantes:
- Los `ST` de Samsung y LG no tienen documentación oficial (D-11) y salen de bibliotecas abiertas: hay que confirmarlos con tus teles.
- En este hito no se lee el nombre por HTTP sin cifrar (D-14).
- Las teles sin driver ya se pueden guardar (D-18).

Pendientes y riesgos:
- **Primera compilación de los hitos 1 y 2.** El riesgo del hito 2 está en las APIs de Android que no pude compilar aquí: `NsdManager` (`hostAddresses`, `stopServiceResolution`, API 34+), `LifecycleEventEffect` y `Settings.Panel.ACTION_WIFI`. La lógica pura y las corrutinas sí compilan.
- Algunos fabricantes filtran la multidifusión aun con el bloqueo tomado. Si una tele no aparece y el registro no muestra respuestas SSDP, ese es el primer sospechoso.
- Android TV en reposo profundo puede no anunciarse por mDNS. La IP manual cubre ese caso.
- El permiso de red local de Android 17 está preparado pero no probado. Cuando se suba a `targetSdk` 37, seguir los 3 pasos del comentario de `LocalNetworkAccess`.

Preguntas para mí:
1. ¿Compilaron los hitos 1 y 2? Si no, pásame el primer error.
2. En la prueba 2, ¿qué teles aparecieron y con qué plataforma? Con eso decido si el hito 3 empieza por Android TV o por Samsung.
3. Las ramas: ¿fusiono en `development` y etiqueto ahí `hito-1` y `hito-2`, o prefieres que pasen por `testing` antes de `main`?
4. ¿Sigues con el nombre "Remoto" y el paquete `app.remoto.control`?
