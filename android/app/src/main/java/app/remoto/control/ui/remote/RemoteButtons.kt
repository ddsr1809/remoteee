package app.remoto.control.ui.remote

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Tamaño mínimo de los botones del control (por encima de los 48 dp de accesibilidad). */
val RemoteButtonSize: Dp = 56.dp

private const val REPEAT_INITIAL_DELAY_MS = 450L
private const val REPEAT_INTERVAL_MS = 120L

/**
 * Botón del control.
 *
 * Con [repeatable], mantenerlo pulsado repite la orden (volumen, canales, flechas):
 * la primera se envía al tocar y las siguientes con `longPress = true`.
 * Con TalkBack (que activa con un clic, sin "pulsación"), se envía una sola vez.
 */
@Composable
fun RemoteButton(
    icon: ImageVector,
    contentDescription: String,
    onPress: (longPress: Boolean) -> Unit,
    haptic: () -> Unit,
    modifier: Modifier = Modifier,
    repeatable: Boolean = false,
    emphasized: Boolean = false,
    size: Dp = RemoteButtonSize,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val currentOnPress by rememberUpdatedState(onPress)
    val currentHaptic by rememberUpdatedState(haptic)
    var sentByPress by remember { mutableStateOf(false) }

    if (repeatable) {
        LaunchedEffect(pressed) {
            if (pressed) {
                sentByPress = true
                currentHaptic()
                currentOnPress(false)
                delay(REPEAT_INITIAL_DELAY_MS)
                while (true) {
                    currentOnPress(true)
                    delay(REPEAT_INTERVAL_MS)
                }
            } else {
                sentByPress = false
            }
        }
    }

    val onClick: () -> Unit = {
        if (repeatable && sentByPress) {
            sentByPress = false
        } else {
            currentHaptic()
            currentOnPress(false)
        }
    }
    val content: @Composable () -> Unit = { Icon(icon, contentDescription = contentDescription) }

    if (emphasized) {
        FilledIconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            interactionSource = interaction,
            content = content,
        )
    } else {
        FilledTonalIconButton(
            onClick = onClick,
            modifier = modifier.size(size),
            interactionSource = interaction,
            content = content,
        )
    }
}
