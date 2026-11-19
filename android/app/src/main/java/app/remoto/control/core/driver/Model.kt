package app.remoto.control.core.driver

/** Plataforma de un dispositivo. Cada una tiene su driver en `drivers/`. */
enum class Platform {
    DEMO,
    ANDROID_TV,
    SAMSUNG,
    LG,
    ROKU,
    INFRARED,
}

/**
 * Lo que un driver sabe hacer. La interfaz oculta los controles cuya capacidad
 * no declara el driver activo.
 */
enum class Capability {
    NAVIGATION,
    VOLUME,
    CHANNELS,

    /** Reproducir/pausa. No todas las plataformas lo exponen (por ejemplo, muchos códigos IR). */
    MEDIA,
    KEYBOARD,
    TOUCHPAD,
    APPS,

    /** Encender una tele apagada (Wake-on-LAN o IR). */
    POWER_ON,

    /** Apagar (o alternar encendido) con una tecla. */
    POWER_OFF,
}

/**
 * Teclas lógicas del control. Cada driver las traduce a su protocolo.
 *
 * @property capability capacidad que el driver debe declarar para aceptar la tecla.
 * @property repeatable si mantener pulsado repite la tecla (volumen, canales, flechas).
 */
enum class RemoteKey(val capability: Capability, val repeatable: Boolean = false) {
    UP(Capability.NAVIGATION, repeatable = true),
    DOWN(Capability.NAVIGATION, repeatable = true),
    LEFT(Capability.NAVIGATION, repeatable = true),
    RIGHT(Capability.NAVIGATION, repeatable = true),
    OK(Capability.NAVIGATION),
    BACK(Capability.NAVIGATION),
    HOME(Capability.NAVIGATION),
    VOLUME_UP(Capability.VOLUME, repeatable = true),
    VOLUME_DOWN(Capability.VOLUME, repeatable = true),
    MUTE(Capability.VOLUME),
    CHANNEL_UP(Capability.CHANNELS, repeatable = true),
    CHANNEL_DOWN(Capability.CHANNELS, repeatable = true),
    PLAY_PAUSE(Capability.MEDIA),
    POWER(Capability.POWER_OFF),
}

/** Cómo empareja la tele la primera vez. */
enum class PairingType {
    /** No hace falta emparejar. */
    NONE,

    /** La tele muestra un código que el usuario escribe en el teléfono (Android TV, LG con PIN). */
    CODE_ON_TV,

    /** La tele muestra un aviso que el usuario acepta con el control original (Samsung, LG). */
    CONFIRM_ON_TV,
}

/** Por qué falló una conexión. Cada motivo tiene un tema de ayuda (ver [HelpTopic.forFailure]). */
enum class FailureReason {
    TIMEOUT,
    PAIRING_REJECTED,
    LOCAL_NETWORK_PERMISSION_DENIED,
    NETWORK_UNAVAILABLE,
    DEVICE_UNREACHABLE,
    PROTOCOL_ERROR,
    UNKNOWN,
}

/**
 * Temas de la pantalla de ayuda. El núcleo solo conoce el identificador; los textos
 * viven en `strings.xml` y los resuelve la capa de interfaz (`ui/help/HelpContent.kt`).
 */
enum class HelpTopic {
    GETTING_STARTED,
    DEMO_MODE,
    CONNECTION_TIMEOUT,
    PAIRING_REJECTED,
    LOCAL_NETWORK_PERMISSION,
    NO_WIFI,
    DEVICE_UNREACHABLE,

    /** Consejos cuando la búsqueda no encuentra la tele (pantalla "No aparece mi tele"). */
    TV_NOT_FOUND,
    ;

    companion object {
        fun forFailure(reason: FailureReason): HelpTopic = when (reason) {
            FailureReason.TIMEOUT -> CONNECTION_TIMEOUT
            FailureReason.PAIRING_REJECTED -> PAIRING_REJECTED
            FailureReason.LOCAL_NETWORK_PERMISSION_DENIED -> LOCAL_NETWORK_PERMISSION
            FailureReason.NETWORK_UNAVAILABLE -> NO_WIFI
            FailureReason.DEVICE_UNREACHABLE -> DEVICE_UNREACHABLE
            FailureReason.PROTOCOL_ERROR, FailureReason.UNKNOWN -> GETTING_STARTED
        }
    }
}

/** Una app que se puede abrir en la tele. */
data class TvApp(val id: String, val name: String)

/**
 * Un dispositivo guardado, tal como lo ve el núcleo. Sin anotaciones de serialización:
 * el formato en disco es asunto de `core/storage`.
 *
 * @property name nombre puesto por el usuario. Vacío = usar el nombre por defecto de la plataforma.
 * @property host IP o nombre de red; `null` en demo e infrarrojo.
 * @property uniqueId identificador que anuncia la propia tele al buscarla (por ejemplo, el
 *   `uuid` de SSDP). Sirve para reconocerla aunque el router le cambie la IP. `null` si se
 *   agregó por IP manual o la tele no anuncia ninguno.
 */
data class TvDevice(
    val id: String,
    val name: String,
    val platform: Platform,
    val host: String? = null,
    val uniqueId: String? = null,
)
