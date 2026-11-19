# Versiones verificadas (Hito 0)

Verificadas el **18 de noviembre de 2026** en las páginas oficiales de cada proyecto. Solo versiones estables, salvo donde se indica.

Desde el entorno donde se escribió el código no había acceso a Maven Central ni al Maven de Google, así que **nada se ha descargado ni compilado todavía**. Si al sincronizar Gradle alguna versión no resuelve, la fuente de la tabla dice dónde volver a mirar.

## Build

| Pieza | Versión | Fuente oficial | Notas |
|---|---|---|---|
| Android Gradle Plugin (AGP) | **9.5.1** | [developer.android.com/build/releases/gradle-plugin](https://developer.android.com/build/releases/gradle-plugin) | Pide Gradle ≥ 9.6.0 y JDK 17; soporta hasta API 37. Trae Kotlin integrado ("built-in Kotlin"). |
| Gradle (wrapper) | **9.9.1** | [docs.gradle.org/current/release-notes.html](https://docs.gradle.org/current/release-notes.html) | 9.9.0 salió el 12-nov-2026. Si diera problemas con AGP, bajar a 9.6.0 (la versión por defecto de AGP 9.5). |
| Kotlin (KGP, compilador Compose, serialización) | **2.4.21** | [kotlinlang.org/docs/releases.html](https://kotlinlang.org/docs/releases.html) | Bugfix del 8-oct-2026. Se fija en el classpath raíz, como indica la guía de AGP 9. |
| KSP | **2.3.14** | [github.com/google/ksp/releases](https://github.com/google/ksp/releases) | Con AGP 9 hace falta KSP ≥ 2.3.6 ([guía de migración AGP 9](https://developer.android.com/agents/skills/build/agp/agp-9-upgrade/skill)). Visto a través del espejo de releases; **verificar** en GitHub al sincronizar. |
| ktlint Gradle plugin | **14.2.0** | [plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint](https://plugins.gradle.org/plugin/org.jlleitschuh.gradle.ktlint) | Usa la versión de ktlint que trae por defecto. |

## Bibliotecas de la app

| Pieza | Versión | Fuente oficial | Notas |
|---|---|---|---|
| Compose BOM | **2026.09.00** | [developer.android.com/develop/ui/compose/bom/bom-mapping](https://developer.android.com/develop/ui/compose/bom/bom-mapping) | Compose 1.12.1, Material 3 1.4.0, iconos 1.7.8. **Compose 1.12 exige `compileSdk` 37** ([blog de Android, ago-2026](https://android-developers.googleblog.com/2026/08/jetpack-compose-august-2026-release.html)). |
| androidx.activity | **1.13.0** | [developer.android.com/jetpack/androidx/versions](https://developer.android.com/jetpack/androidx/versions) | |
| androidx.core | **1.19.1** | ídem | |
| androidx.lifecycle | **2.11.0** | ídem | viewmodel-compose, runtime-compose, process |
| androidx.navigation | **2.10.2** | ídem | Rutas con tipos (kotlinx.serialization) |
| androidx.datastore | **1.3.0** | ídem | Preferences DataStore |
| androidx.hilt | **1.4.0** | ídem | `hilt-lifecycle-viewmodel-compose` (aquí vive `hiltViewModel()` desde 1.3) |
| Dagger / Hilt | **2.60.1** | [github.com/google/dagger/releases](https://github.com/google/dagger/releases) | Con AGP 9 hace falta Hilt ≥ 2.59.2. |
| kotlinx-coroutines | **1.11.0** | [github.com/Kotlin/kotlinx.coroutines/releases](https://github.com/Kotlin/kotlinx.coroutines/releases) | Visto en Maven Central (may-2026). **Verificar** si hay 1.11.x. |
| kotlinx-serialization | **1.12.0** | [central.sonatype.com (kotlinx-serialization-bom)](https://central.sonatype.com/artifact/org.jetbrains.kotlinx/kotlinx-serialization-bom) | |
| Google Tink (tink-android) | **1.24.0** | [github.com/tink-crypto/tink-java/releases](https://github.com/tink-crypto/tink-java/releases) | minSdk de Tink: 23 (planean subir a 24). |

## Para hitos posteriores (no se usan todavía)

| Pieza | Versión | Fuente oficial | Hito |
|---|---|---|---|
| OkHttp / mockwebserver3 | **5.4.0** | [github.com/square/okhttp](https://github.com/square/okhttp) (README) | H3 |
| Protobuf Gradle plugin | **0.9.6** | [plugins.gradle.org/plugin/com.google.protobuf](https://plugins.gradle.org/plugin/com.google.protobuf) | H3 |
| Protobuf (runtime javalite / kotlin-lite) | **v35.0** (artefacto Java `4.35.x`) | [github.com/protocolbuffers/protobuf/releases](https://github.com/protocolbuffers/protobuf/releases) | H3 — **verificar** el número exacto del artefacto Java al empezar el hito |
| Google Play Billing Library | **9.1.0** | [developer.android.com/google/play/billing/release-notes](https://developer.android.com/google/play/billing/release-notes) | H6 |
| Google Mobile Ads (legado) | 25.4.0 | [developers.google.com/admob/android/rel-notes](https://developers.google.com/admob/android/rel-notes) | H6 — ver decisión abajo |
| GMA Next-Gen SDK | 1.6.0 | [developers.google.com/admob/android/next-gen/rel-notes](https://developers.google.com/admob/android/next-gen/rel-notes) | H6 — SDK preferido por Google desde el 6-jul-2026 |
| UMP (consentimiento) | **4.0.0** | [developers.google.com/admob/android/privacy/release-notes](https://developers.google.com/admob/android/privacy/release-notes) | H6 — minSdk 23 |

## Pruebas

| Pieza | Versión | Fuente oficial |
|---|---|---|
| JUnit 4 | **4.13.2** | [junit.org/junit4](https://junit.org/junit4/) (en modo mantenimiento; es la última) |
| kotlinx-coroutines-test | **1.11.0** | igual que coroutines |
| Turbine | **1.2.1** | [github.com/cashapp/turbine](https://github.com/cashapp/turbine) (README) |
| MockWebServer | **5.4.0** | igual que OkHttp (H3) |

## Datos de Google Play que condicionan versiones

- **Play Billing Library:** desde el 31-ago-2026 Google Play solo acepta apps nuevas y actualizaciones con **Billing 8 o superior**; la prórroga terminó el 1-nov-2026. Billing 8 deja de aceptarse el 31-ago-2027, así que usaremos **9.x** ([tabla de obsolescencia](https://developer.android.com/google/play/billing/deprecation-faq)).
- **targetSdk:** 36 (lo exige Google Play desde el 31-ago-2026).
- **compileSdk:** 37. No es lo mismo que targetSdk: compilar contra 37 no cambia el comportamiento en tiempo de ejecución, solo permite usar Compose 1.12. Ver decisión D-03 en `PROGRESO.md`.

## Cómo actualizar esta tabla

1. Revisar las fuentes de arriba (no blogs de terceros).
2. Cambiar `gradle/libs.versions.toml`.
3. `./gradlew assembleDebug test` y una prueba manual del modo demostración.
4. Anotar la fecha y el cambio aquí.
