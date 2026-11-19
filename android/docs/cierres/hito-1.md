## Cierre del Hito 1: Base y tele simulada

Estado: completo con pendientes — el código está escrito y la lógica verificada, pero **no se pudo compilar la app Android** en el entorno de desarrollo (sin acceso a Maven Central ni al Maven de Google, así que ni el SDK ni las dependencias se pudieron descargar). La primera compilación tiene que ser en tu equipo.

Criterios de aceptación:
- ❌ (sin verificar todavía) `./gradlew assembleDebug test` termina sin errores — falta correrlo con el SDK. Lo que sí se compiló: núcleo, sesión, driver demo y *fakes* de prueba, con el compilador de Kotlin; un arnés ejecutó 44 comprobaciones equivalentes a las pruebas y todas pasan.
- ❌ (sin verificar en teléfono) Desde la primera apertura llego al control de la tele simulada y cada botón produce una reacción visible — flujo implementado (Bienvenida → "Probar sin tele" → Control). Verificado en lógica: las 14 teclas cambian la pantalla simulada y su contador; la tele muestra un aviso con la tecla recibida.
- ❌ (sin verificar en teléfono) Cada error simulado muestra su pantalla de ayuda — el menú del insecto provoca los 3 errores; verificado en lógica que cada uno termina en `Failed` con su `HelpTopic`, y la pantalla de error muestra ese tema completo.
- ✅ Cambiar el idioma del teléfono cambia todos los textos — 0 textos fijos en el código; 124 claves presentes en `values/` y `values-es/`, comprobado con un script.
- ✅ La pantalla del control no importa nada del paquete `drivers/` — `grep -rn "import app.remoto.control.drivers" ui/` no devuelve nada.

Qué cambió:
- Proyecto Gradle (Kotlin DSL, catálogo de versiones, wrapper 9.9.1), AGP 9.5.1 con Kotlin integrado 2.4.21, Hilt + KSP, Compose BOM 2026.09.00.
- Núcleo: `TvDriver`, `Capability`, `RemoteKey`, `ConnectionState`, `PairingType`, `FailureReason`, `HelpTopic`, `TvDevice`, `SimulatedTv` y `RemoteSession` (driver activo, filtro por capacidades, reconexión al volver a primer plano).
- Almacenamiento: ajustes y dispositivos en DataStore; secretos cifrados con Tink + Android Keystore, excluidos de las copias de seguridad.
- Driver demo: tele simulada con emparejamiento por código, 14 teclas, texto, touchpad, apps, encendido y 3 errores simulados.
- Pantallas: bienvenida, inicio, próximamente (Wi-Fi e IR), control (cruceta, volumen, canales, silencio, reproducir/pausa, encendido, teclado, touchpad, apps), emparejamiento, error con ayuda, dispositivos guardados (abrir, renombrar, borrar), ajustes (vibración, tema, idioma, aviso de no afiliación, versión) y ayuda (7 temas).
- Tema Material 3 claro/oscuro con color dinámico; textos en inglés y español; idioma por app en Android 13+.
- 39 pruebas unitarias (JUnit 4, coroutines-test, Turbine).

Cómo compilar y probar:
1. Abre la carpeta en Android Studio (la última estable). Acepta instalar la plataforma **API 37** si la pide.
2. En la terminal del proyecto: `./gradlew assembleDebug test`
3. Con un teléfono conectado por USB y la depuración activada: `./gradlew installDebug`
4. Opcional: `./gradlew ktlintFormat` y luego `./gradlew ktlintCheck` (el formato no se pudo pasar por ktlint aquí).
5. Si algo no compila, copia el primer error completo y me lo pasas: lo más probable son ajustes pequeños por versiones que no pude descargar.

Pruebas manuales que debo hacer yo:
1. Instala y abre la app por primera vez → aparece la bienvenida con tres opciones.
2. Toca "Buscar en mi Wi-Fi" → pantalla "Próximamente" con botón para probar sin tele. Atrás. Lo mismo con "Usar infrarrojo".
3. Toca "Probar sin tele" → se abre el control y la tele simulada muestra un código de 4 dígitos.
4. Escribe un código equivocado → pantalla "La tele rechazó el emparejamiento" con su explicación y "Reintentar".
5. Reintenta y escribe el código correcto → aparece la tele simulada con la rejilla de 8 apps y el control debajo.
6. Pulsa cada botón: flechas (el foco se mueve), OK (abre la app), atrás e inicio (vuelve), volumen (barra de volumen), silencio (ícono tachado), canales (cambia "CH"), reproducir/pausa, encendido (se apaga y se vuelve a encender). Cada pulsación muestra un aviso con el nombre de la tecla arriba a la izquierda.
7. Mantén pulsado subir volumen → sube de forma continua; al soltar se detiene.
8. Teclado: escribe "hola" y envía → aparece abajo en la tele. Touchpad: desliza → se mueve un punto blanco; toca → abre la app bajo el punto. Apps: elige una → se abre.
9. Ícono del insecto → "Tiempo de conexión agotado" → tras unos segundos, pantalla de ayuda de ese error. Reintentar → vuelve el control. Repite con "Emparejamiento rechazado" y "Permiso denegado".
10. Cierra la app desde recientes y ábrela de nuevo → se abre directamente el control de la tele simulada **sin pedir código**; atrás lleva al inicio.
11. Manda la app a segundo plano 10 s y vuelve → reconecta sola.
12. Ajustes → desactiva "Vibrar al tocar" → los botones dejan de vibrar. Cambia el tema a Oscuro y a Claro.
13. Cambia el idioma del teléfono a inglés (o el idioma de la app en Ajustes del sistema › Apps › Remoto › Idioma) → todos los textos cambian.
14. Teles guardadas → renombra la tele simulada → el nombre nuevo aparece en el control. Bórrala → al volver a "Probar sin tele" pide código otra vez.
15. Con TalkBack activo, recorre el control → cada botón se anuncia con su nombre ("Subir volumen", "Canal siguiente"…).
16. Saca capturas de inicio, control y ajustes en claro y oscuro (también puedes usar las vistas previas `@Preview` de Android Studio).

Decisiones que tomé: D-07 a D-10 en `PROGRESO.md`. Además: el control se abre solo al arrancar si hay una tele usada antes (atrás lleva al inicio); la tele simulada pide código la primera vez para ejercitar el flujo de emparejamiento que usará Android TV; sin permisos en el manifiesto mientras el modo demo no use la red.

Pendientes y riesgos:
- **Primera compilación.** Puntos con más riesgo: el DSL de AGP 9 (`compileSdk = 37` como entero), nombres de algunos íconos `AutoMirrored`, la API de Tink (`RegistryConfiguration`) y Gradle 9.9.1 con AGP 9.5 (si falla, bajar el wrapper a 9.6.0).
- ktlint no se ejecutó; `ktlintFormat` arreglará casi todo lo de formato.
- Faltan el APK y las capturas (dependen de compilar).
- El nombre "Remoto" y el paquete `app.remoto.control` son provisionales; cambiarlos es gratis hasta la primera subida a Play.
- Si el teléfono de pruebas tiene emisor IR, conviene decidir si el hito 5 se adelanta.

Preguntas para mí:
1. ¿Te quedas con el nombre "Remoto" y el paquete `app.remoto.control`, o prefieres otros?
2. ¿Qué tele(s) y qué teléfono tienes para probar? Decide si el hito 3 empieza por Android TV o por Samsung.
3. ¿Quieres que el repositorio viva en GitHub? Si me dices cuál, lo subo con todas las ramas y etiquetas.
4. Tras compilar y hacer las pruebas manuales, ¿apruebas el cierre para fusionar a `main` y crear la etiqueta `hito-1`?
