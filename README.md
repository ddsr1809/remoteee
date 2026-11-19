# Remoto: control remoto universal para TV

App Android nativa (Kotlin + Jetpack Compose) que convierte el teléfono en el control remoto de la tele:

- **Wi-Fi (red local):** habla con la tele o el reproductor usando el protocolo de cada plataforma (Android TV / Google TV, Samsung Tizen, LG webOS, Roku).
- **Infrarrojo:** usa el emisor IR del teléfono cuando existe.
- **Modo demostración:** una tele simulada para probar la app sin hardware (la usan también los revisores de Google Play).

Todo ocurre entre el teléfono y la tele: no hay servidor propio, cuentas ni nube.

> "Remoto" es un nombre de trabajo. El `applicationId` (`app.remoto.control`) se puede cambiar sin costo hasta la primera subida a Google Play; después ya no.

## Requisitos

| Herramienta | Versión |
|---|---|
| Android Studio | La última estable (debe traer soporte para AGP 9.5) |
| JDK | 17 o superior (el que trae Android Studio sirve) |
| Android SDK | Plataforma **API 37** instalada (Compose 1.12 la exige para compilar) |

Las versiones exactas de cada dependencia están en [`gradle/libs.versions.toml`](gradle/libs.versions.toml) y su verificación en [`docs/VERSIONES.md`](docs/VERSIONES.md).

## Compilar y probar

El proyecto Gradle está en la carpeta `android/` (ábrela en Android Studio).

```bash
cd android
# Primera vez: Android Studio crea local.properties con la ruta del SDK.
./gradlew assembleDebug test     # APK de depuración + pruebas unitarias
./gradlew ktlintCheck            # estilo de código
./gradlew installDebug           # instalar en un teléfono conectado por USB
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Estructura

```
app/src/main/java/app/remoto/control/
  core/        modelo común, sesión, almacenamiento, red, diagnóstico (no conoce marcas)
  drivers/     un paquete por plataforma (demo, androidtv, samsung, lg, roku, ir)
  ui/          pantallas Compose (no importa nada de drivers/)
  ads/         anuncios y consentimiento (hito 6)
  billing/     compra "Pro" (hito 6)
```

Regla central: **la pantalla del control no sabe qué marca maneja.** Habla con la interfaz `TvDriver` y cada driver traduce al protocolo de su plataforma.

## Cómo se trabaja

El proyecto avanza por hitos. El estado actual, las tareas y las decisiones están en [`PROGRESO.md`](PROGRESO.md). Cada hito cerrado tiene su etiqueta de git (`hito-0`, `hito-1`, …).
