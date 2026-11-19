package app.remoto.control.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import app.remoto.control.R
import app.remoto.control.core.driver.Platform
import app.remoto.control.core.driver.TvDevice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemotoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                }
            }
        },
        actions = actions,
    )
}

/** Nombre que se muestra: el que puso el usuario o, si está vacío, el de la plataforma. */
@Composable
fun deviceDisplayName(device: TvDevice): String =
    device.name.ifBlank { stringResource(defaultNameRes(device.platform)) }

fun defaultNameRes(platform: Platform): Int = when (platform) {
    Platform.DEMO -> R.string.device_default_demo
    else -> platformNameRes(platform)
}

fun platformNameRes(platform: Platform): Int = when (platform) {
    Platform.DEMO -> R.string.platform_demo
    Platform.ANDROID_TV -> R.string.platform_android_tv
    Platform.SAMSUNG -> R.string.platform_samsung
    Platform.LG -> R.string.platform_lg
    Platform.ROKU -> R.string.platform_roku
    Platform.INFRARED -> R.string.platform_infrared
}

/**
 * Vibración breve al tocar un botón. Usa la respuesta háptica del sistema, que no
 * necesita el permiso VIBRATE (decisión D-10) y respeta los ajustes del teléfono.
 */
@Composable
fun rememberHaptics(enabled: Boolean): () -> Unit {
    val view = LocalView.current
    return remember(view, enabled) {
        { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY) }
    }
}
