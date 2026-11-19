package app.remoto.control.ui

import kotlinx.serialization.Serializable

// Rutas de navegación con tipos (Navigation 2.8+). Los argumentos se guardan en el
// SavedStateHandle de cada pantalla con el nombre de la propiedad.

@Serializable
data object WelcomeRoute

@Serializable
data object HomeRoute

@Serializable
data class RemoteRoute(val deviceId: String)

@Serializable
data object DiscoverRoute

@Serializable
data object NotFoundRoute

@Serializable
data object ManualAddRoute

@Serializable
data object DevicesRoute

@Serializable
data object SettingsRoute

/** [topic] es el `name` de un `HelpTopic`; `null` muestra la lista de temas. */
@Serializable
data class HelpRoute(val topic: String? = null)

/** [feature] es el `name` de un [ComingSoonFeature]. */
@Serializable
data class ComingSoonRoute(val feature: String)

/** Wi-Fi ya no está aquí desde el hito 2; infrarrojo llega en el hito 5. */
enum class ComingSoonFeature { INFRARED }
