# Progreso del proyecto

**Hito actual:** 2 — Descubrimiento
**Estado:** código completo, pendiente de compilar en un equipo con Android SDK y de las pruebas manuales en la red del dueño. El hito 1 también espera su primera compilación (ver abajo).

> El proyecto Gradle vive en `android/` desde que el repositorio pasó a GitHub (`ddsr1809/remoteee`, rama `development`).

> Si una sesión nueva retoma el trabajo: leer primero `prompt_control_remoto_android.md` (instrucciones del proyecto) y después este archivo. Continuar desde la última tarea marcada.

---

## Datos del proyecto (sección 2 del prompt)

El dueño pidió que yo respondiera las preguntas del hito 0. Estas son las suposiciones; cualquiera se puede cambiar.

| Dato | Valor supuesto | Cómo cambiarlo |
|---|---|---|
| Nombre de la app | **Remoto** (en la tienda: "Remoto: control para TV", 24 caracteres) | `app_name` en `strings.xml`. Antes de publicar, revisar que no choque con otra app. |
| Paquete (applicationId) | **`app.remoto.control`** | `applicationId` y `namespace` en `app/build.gradle.kts` + mover paquetes. **Gratis hasta la primera subida a Play; después es permanente.** |
| Idiomas | inglés (`values/`) y español (`values-es/`) | — |
| Hardware de prueba | Desconocido. Supongo un teléfono Android sin emisor IR y ninguna tele concreta. | Decidir antes del hito 3 qué driver va primero. |
| Política de privacidad | Pendiente hasta el hito 6 | — |
| Horas por semana | Supongo **10 h** | Ver nota de estimación abajo. |
| Repositorio | Git local, entregado como `.zip` con historial y etiquetas | Se puede subir tal cual a GitHub (`git remote add origin … && git push --all && git push --tags`). |

**Nota de estimación (10 h/semana):** las duraciones del mapa de hitos son realistas a ese ritmo para H0–H2 y H5–H7. H3 (primer driver real) y H4 (tres drivers) dependen mucho del hardware disponible: sin la tele física, H3 no puede cerrarse. El total de 12–16 semanas se sostiene si el hardware está a mano al llegar a H3.

---

## Decisiones

| # | Fecha | Decisión | Motivo |
|---|---|---|---|
| D-01 | 2026-11-18 | **`minSdk` 26** (Android 8.0) | Cubre casi todos los teléfonos en uso. Por debajo, Tink (≥23), UMP 4 (≥23) y las bibliotecas androidx (≥23) ya no ayudan, y 26 trae íconos adaptables, `java.util.Base64` y APIs de red más estables. Bajar a 24 sería posible si hiciera falta. |
| D-02 | 2026-11-18 | **ktlint** (plugin `org.jlleitschuh.gradle.ktlint`), no detekt | detekt 1.23.8 (la última estable) está compilado contra un Kotlin viejo y falla con Kotlin 2.4; detekt 2.0 sigue en alfa. ktlint no depende de la versión del compilador y cubre lo que más importa en un equipo de una persona: formato uniforme. Se puede sumar detekt cuando la 2.0 sea estable. |
| D-03 | 2026-11-18 | **`compileSdk` 37, `targetSdk` 36** (el prompt pedía 36 y 36) | Compose 1.12 (la versión estable actual) exige compilar contra API 37. `compileSdk` solo decide contra qué APIs compila el código; el comportamiento en el teléfono lo decide `targetSdk`, que sigue en 36. Por eso tampoco se activa todavía el permiso `ACCESS_LOCAL_NETWORK` de Android 17. Alternativa descartada: quedarse en un BOM viejo de Compose (1.11). |
| D-04 | 2026-11-18 | **AGP 9 con Kotlin integrado**: no se aplica el plugin `org.jetbrains.kotlin.android`; la versión de Kotlin se fija en el `buildscript` raíz | Es lo que pide la guía oficial de AGP 9. Hilt 2.60.1 y KSP 2.3.14 cumplen los mínimos (2.59.2 y 2.3.6). |
| D-05 | 2026-11-18 | **Play Billing 9.1.0** (hito 6) | Google Play exige ≥ 8 desde el 31-ago-2026 y la 8 caduca en ago-2027. |
| D-06 | 2026-11-18 | Anuncios: decidir en H6 entre GMA legado 25.x y **GMA Next-Gen** | Google declaró Next-Gen como SDK preferido el 6-jul-2026 y el 25.x como legado. Hay que revisar en H6 que Next-Gen cubra los formatos que usamos (apertura, intersticial, banner, recompensado) y UMP. |
| D-07 | 2026-11-18 | Ajustes a la interfaz común `TvDriver` (ver `core/driver/TvDriver.kt`) | Se añadió `Capability.MEDIA` (reproducir/pausa no lo tienen todos), `RemoteKey.POWER` (apagar es una tecla; encender es `powerOn()` por Wake-on-LAN), `movePointer()`/`tap()` para el touchpad y `listApps()` (Android TV no da la lista, así que puede devolver vacío). |
| D-08 | 2026-11-18 | El modo demostración vive detrás de una interfaz de núcleo `SimulatedTv` | La pantalla del control necesita dibujar la tele simulada y simular errores sin importar `drivers/demo`. La sesión expone `SimulatedTv?`; solo el driver demo lo implementa. |
| D-09 | 2026-11-18 | Secretos: Tink AEAD (AES-256-GCM) con clave maestra en Android Keystore; el texto cifrado va en un Preferences DataStore propio | Como pide el prompt. Tink guarda su propio *keyset* (ya cifrado por Keystore) en SharedPreferences: así funciona `AndroidKeysetManager`. Ambos archivos quedan **fuera de las copias de seguridad**, porque la clave de Keystore no viaja a otro teléfono y el cifrado restaurado sería ilegible. |
| D-10 | 2026-11-18 | Hápticos con `View.performHapticFeedback` | No requiere el permiso `VIBRATE`, que no está en la lista de la sección 7.4. |
| D-11 | 2026-11-18 | SSDP pide un `ST` por plataforma (`roku:ecp`, `urn:samsung.com:device:RemoteControlReceiver:1`, `urn:lge-com:service:webos-second-screen:1`), no `ssdp:all` | Con `ssdp:all` contestan routers, impresoras y altavoces; con tipos concretos solo contestan teles y la plataforma sale del propio `ST`. Roku está documentado; **Samsung y LG salen de bibliotecas de código abierto y hay que confirmarlos con hardware real**. |
| D-12 | 2026-11-18 | Android TV se busca solo por mDNS (`_androidtvremote2._tcp`) con `NsdManager` | Es el servicio del protocolo que usará su driver y trae el nombre de la tele. `NsdManager` viene en Android y no necesita el bloqueo de multidifusión. Las resoluciones van en fila porque Android 13 y anteriores solo admiten una a la vez. |
| D-13 | 2026-11-18 | Dos resultados son la misma tele si comparten IP **o** identificador; si las vías discrepan en la plataforma, gana Android TV | El identificador cubre teles con Wi-Fi y cable a la vez. El servicio mDNS de Android TV es más específico que cualquier pista de SSDP. |
| D-14 | 2026-11-18 | En el hito 2 **no** se descarga la descripción UPnP (`LOCATION`) para leer el nombre de la tele | Es HTTP sin cifrar a la red local y Android lo bloquea por defecto; abrirlo es una decisión del hito 4 (Roku) con su configuración mínima. El nombre llega por mDNS (Android TV) o lo pondrá cada driver (Samsung `/api/v2/`, Roku `device-info`, LG SSAP); mientras, se muestra la plataforma y el usuario puede renombrar. |
| D-15 | 2026-11-18 | Permisos nuevos: solo `INTERNET`, `ACCESS_NETWORK_STATE` y `CHANGE_WIFI_MULTICAST_STATE` | Son los únicos que usa la búsqueda. `ACCESS_WIFI_STATE` no hace falta todavía (no se lee el nombre ni la IP de la Wi-Fi). Sigue sin declararse `ACCESS_LOCAL_NETWORK` (targetSdk 36). |
| D-16 | 2026-11-18 | El socket SSDP se ata a la red Wi-Fi con `Network.bindSocket`, eligiendo una red Wi-Fi o Ethernet que no sea VPN | Con datos móviles o una VPN activa, el `M-SEARCH` podría salir por otra red y no encontrar nada. |
| D-17 | 2026-11-18 | Registro de diagnóstico solo en memoria (300 entradas), con redacción al entrar; la cabecera lleva versión de la app, de Android y fabricante/modelo del teléfono | Se oculta lo que identifica a la persona o su red (IP a medias, IPv6, MAC, UUID acortado) y nunca se registran nombres de teles. El modelo del teléfono no es un dato personal y explica fallos de multidifusión de algunos fabricantes. Nunca se envía solo: el usuario lo copia. |
| D-18 | 2026-11-18 | Las teles sin driver se pueden guardar ya; el control muestra "llega en una próxima versión" | Así el flujo buscar → guardar queda probado de punta a punta y en el hito 3 solo cambia lo que pasa al abrir el control. |
| D-19 | 2026-11-18 | "Agregar por IP" acepta solo IPv4 y pide elegir la plataforma | Sin drivers no hay forma fiable de preguntarle a la tele qué es. Se rechazan direcciones que nunca son una tele y se avisa (sin bloquear) si no es de una red privada. |
| D-20 | 2026-11-18 | `TvDevice.uniqueId` opcional y `RemoteSession` recibe `DiagnosticsLog` sin valor por defecto | Lo guardado en el hito 1 se sigue leyendo (campo ausente = `null`). Sin valor por defecto se evita cualquier duda de Dagger con el constructor sintético que genera Kotlin. |

---

## Hito 0 — Preparación ✅ (etiqueta `hito-0`)

- [x] Preguntas de la sección 2 → el dueño pidió que las respondiera yo; ver "Datos del proyecto".
- [x] Proponer `minSdk` con justificación → D-01.
- [x] Verificar la última versión estable de cada dependencia → `docs/VERSIONES.md`.
- [x] Versión mínima de Play Billing que exige Google Play hoy → 8 (D-05).
- [x] ktlint vs detekt → D-02.
- [x] Árbol de archivos del hito 1 → ver abajo.
- [x] Repositorio con `.gitignore`, `README.md` y `PROGRESO.md`.

### Árbol de archivos del hito 1

```
settings.gradle.kts, build.gradle.kts, gradle.properties, gradle/libs.versions.toml, gradlew
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/app/remoto/control/
  RemotoApp.kt                    Application (Hilt) + reconexión al volver a primer plano
  MainActivity.kt                 única Activity
  di/AppModule.kt                 DataStore, scope de aplicación, enlaces
  core/driver/                    TvDriver, Capability, RemoteKey, ConnectionState, PairingType,
                                  FailureReason, HelpTopic, TvDevice, SimulatedTv, RemoteSession
  core/storage/                   SettingsRepository, DeviceRepository, SecretStore (+ DataStore/Tink)
  core/discovery/ core/net/ core/diagnostics/      (vacíos hasta H2–H3)
  drivers/demo/                   DemoTvDriver, DemoTvDriverFactory, DemoDriverModule
  drivers/androidtv/ samsung/ lg/ roku/ ir/        (vacíos)
  ui/AppNavHost.kt, ui/Routes.kt, ui/theme/
  ui/home/ (bienvenida, inicio, próximamente)  ui/remote/  ui/pairing/
  ui/devices/  ui/settings/  ui/help/  ui/components/
  ads/ billing/                                    (vacíos hasta H6)
app/src/main/res/  values/strings.xml, values-es/strings.xml, xml/, drawable/, mipmap-anydpi-v26/
app/src/test/java/app/remoto/control/   pruebas unitarias
```

---

## Hito 1 — Base y tele simulada ⏳ (código completo; falta compilar y etiquetar `hito-1`)

- [x] Proyecto con Gradle Kotlin DSL y catálogo de versiones; `targetSdk` 36 y `compileSdk` 37 (D-03).
- [x] Hilt, Navigation Compose, tema Material 3 claro y oscuro (con color dinámico en Android 12+).
- [x] `strings.xml` en inglés (`values/`) y español (`values-es/`); ningún texto fijo en el código.
- [x] Paquetes de la sección 7.2 (los vacíos llevan `.gitkeep`).
- [x] Modelo común: `TvDriver`, `Capability`, `RemoteKey`, `ConnectionState`, `PairingType`, `FailureReason`, `HelpTopic`.
- [x] Driver demo con tele simulada que reacciona y errores simulados (tiempo agotado, emparejamiento rechazado, permiso negado).
- [x] Pantallas: bienvenida, inicio, control, emparejamiento, dispositivos guardados, ajustes (sin Pro) y ayuda.
- [x] Primera apertura con "Buscar en mi Wi-Fi", "Usar infrarrojo" (→ "próximamente") y "Probar sin tele".
- [x] Almacenamiento de dispositivos con DataStore.
- [x] Almacén de secretos con Tink y Android Keystore (el demo lo usa para recordar que ya se emparejó).
- [x] Vibración al tocar, desactivable.
- [x] Accesibilidad: descripción en cada botón, objetivos táctiles ≥ 48 dp.
- [x] Pruebas unitarias del ViewModel del control, de la sesión y de los estados de conexión.

---

Pendiente: `./gradlew assembleDebug test` en un equipo con SDK, las 16 pruebas manuales de `android/docs/cierres/hito-1.md` y la aprobación del dueño.

---

## Hito 2 — Descubrimiento ⏳ (código completo; falta compilar y probar en la red del dueño)

- [x] Búsqueda SSDP con `MulticastSocket` y `WifiManager.MulticastLock`, tomado solo durante la búsqueda.
- [x] Búsqueda mDNS con `NsdManager`.
- [x] Lista unificada: un dispositivo aparece una sola vez aunque responda por dos vías.
- [x] Identificación de plataforma (Roku, Samsung, LG, Android TV) a partir de las respuestas.
- [x] Agregar dispositivo por IP manual.
- [x] Detección de "sin Wi-Fi" antes de buscar.
- [x] Capa `LocalNetworkAccess` con la pantalla que explica el permiso de red local, sin activarla.
- [x] Pantalla "No aparece mi tele" con IP manual y consejos.
- [x] Registro de diagnóstico copiable, sin IP completas ni datos personales.
- [x] Pruebas unitarias del analizador SSDP y de la deduplicación.

Cierre: `android/docs/cierres/hito-2.md`.

## Hito 3 — Primer driver real (Android TV o Samsung, según hardware)

- [ ] Estudiar implementaciones de referencia y reportar su licencia antes de usarlas.
- [ ] (Android TV) `.proto` + generación de código; certificado propio cifrado; emparejamiento 6467; control 6466.
- [ ] (Samsung) WSS 8002 + `/api/v2/`; token cifrado; teclas y texto verificados con la tele real; Wake-on-LAN.
- [ ] Confianza TLS solo para ese dispositivo (huella guardada y aviso si cambia).
- [ ] Reconexión automática al volver a primer plano (la base ya existe desde H1).
- [ ] Mensajes de error con su tema de ayuda.
- [ ] Pruebas con servidor simulado.

## Hito 4 — Más plataformas

- [ ] Driver pendiente del H3.
- [ ] Driver LG webOS (registro con manifiesto, `client-key` cifrada, socket de puntero).
- [ ] Driver Roku (ECP, detección de "Control by mobile apps" y guía).
- [ ] Configuración de seguridad de red mínima para HTTP hacia Roku.
- [ ] Guías de ayuda por marca.
- [ ] Pruebas de cada driver contra servidor simulado.
- [ ] Tabla de compatibilidad.

## Hito 5 — Infrarrojo

- [ ] Driver IR con `ConsumerIrManager`; opción visible solo si `hasIrEmitter()`.
- [ ] Manifiesto: `TRANSMIT_IR` y `uses-feature` `consumerir` con `required="false"`.
- [ ] Codificadores NEC y Sony SIRC con pruebas; RC5/RC6 si da tiempo.
- [ ] Formato interno de juegos de códigos y archivo de procedencia.
- [ ] Ninguna base de códigos externa sin aprobación de licencia.
- [ ] Flujo "probar encendido".
- [ ] Mantener pulsado repite el código.

## Hito 6 — Monetización y cumplimiento

- [ ] Anuncios con identificadores de prueba (decidir SDK, D-06).
- [ ] UMP: consentimiento antes de pedir anuncios; opción para revisarlo.
- [ ] Reglas de anuncios en `ads/` con pruebas de frecuencia.
- [ ] Play Billing 9.x: compra "Pro", restauración, verificación al abrir.
- [ ] Con Pro activo, no inicializar anuncios.
- [ ] Desbloqueos temporales con anuncio recompensado.
- [ ] Pantalla de licencias de terceros.
- [ ] Aviso de no afiliación y enlace a la política de privacidad (el aviso ya existe en ajustes desde H1).
- [ ] Borrador del formulario "Seguridad de los datos".

## Hito 7 — Listo para la tienda

- [ ] Firma de release con claves fuera del repositorio.
- [ ] R8 con reglas para protobuf, OkHttp, Tink y SDK de Google; build de release completo.
- [ ] Ícono adaptable definitivo (el de H1 es provisional y propio).
- [ ] Revisión final de permisos.
- [ ] Identificadores reales de anuncios por configuración de build.
- [ ] Textos de la ficha en español e inglés.
- [ ] Capturas con el modo demostración.
- [ ] Instrucciones para revisores.
- [ ] Lista de comprobación de Play Console.
- [ ] Guía para los 12 testers.

---

## Pendientes que pasan al siguiente hito

- Compilar por primera vez (hitos 1 y 2) y corregir lo que salga.
- Confirmar con teles reales los `ST` de Samsung y LG y el servicio mDNS de Android TV (D-11, D-12).
- Confirmar el nombre y el `applicationId` antes de la primera subida a Play.
- Saber qué tele y qué teléfono hay para pruebas antes de H3.
- H3/H4: cada driver debe leer el nombre real de la tele y actualizar el guardado si el usuario no lo renombró (D-14).
