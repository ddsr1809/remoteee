package app.remoto.control.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.remoto.control.R
import app.remoto.control.core.driver.Capability
import app.remoto.control.core.driver.RemoteKey

/** Acciones del control. La pantalla las conecta al ViewModel. */
data class RemoteActions(
    val onKey: (RemoteKey, Boolean) -> Unit,
    val onPower: () -> Unit,
    val onKeyboard: () -> Unit,
    val onToggleTouchpad: () -> Unit,
    val onApps: () -> Unit,
    val onPointerMove: (Float, Float) -> Unit,
    val onPointerTap: () -> Unit,
)

/**
 * Distribución del control (sección 7.6): encendido, atrás e inicio arriba; cruceta y OK
 * al centro; volumen y canales a los lados; fila inferior con teclado, touchpad y apps.
 * Solo se dibuja lo que el driver declara en sus capacidades.
 */
@Composable
fun RemotePad(
    capabilities: Set<Capability>,
    touchpadMode: Boolean,
    actions: RemoteActions,
    haptic: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val has: (Capability) -> Boolean = { it in capabilities }
    val key: (RemoteKey) -> (Boolean) -> Unit = { k -> { long -> actions.onKey(k, long) } }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Fila superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (has(Capability.POWER_OFF) || has(Capability.POWER_ON)) {
                RemoteButton(
                    icon = Icons.Filled.PowerSettingsNew,
                    contentDescription = stringResource(R.string.key_power),
                    onPress = { actions.onPower() },
                    haptic = haptic,
                    emphasized = true,
                )
            } else {
                Spacer(Modifier.size(RemoteButtonSize))
            }
            if (has(Capability.NAVIGATION)) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    RemoteButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.key_back),
                        onPress = key(RemoteKey.BACK),
                        haptic = haptic,
                    )
                    RemoteButton(
                        icon = Icons.Filled.Home,
                        contentDescription = stringResource(R.string.key_home),
                        onPress = key(RemoteKey.HOME),
                        haptic = haptic,
                    )
                }
            }
        }

        // Centro: volumen | cruceta o touchpad | canales
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (has(Capability.VOLUME)) {
                RockerColumn(
                    label = stringResource(R.string.label_volume),
                    upIcon = Icons.AutoMirrored.Filled.VolumeUp,
                    upDescription = stringResource(R.string.key_volume_up),
                    onUp = key(RemoteKey.VOLUME_UP),
                    downIcon = Icons.AutoMirrored.Filled.VolumeDown,
                    downDescription = stringResource(R.string.key_volume_down),
                    onDown = key(RemoteKey.VOLUME_DOWN),
                    haptic = haptic,
                )
            } else {
                Spacer(Modifier.width(RemoteButtonSize))
            }

            if (touchpadMode && has(Capability.TOUCHPAD)) {
                Touchpad(onMove = actions.onPointerMove, onTap = { haptic(); actions.onPointerTap() })
            } else if (has(Capability.NAVIGATION)) {
                DPad(onKey = actions.onKey, haptic = haptic)
            }

            if (has(Capability.CHANNELS)) {
                RockerColumn(
                    label = stringResource(R.string.label_channel),
                    upIcon = Icons.Filled.Add,
                    upDescription = stringResource(R.string.key_channel_up),
                    onUp = key(RemoteKey.CHANNEL_UP),
                    downIcon = Icons.Filled.Remove,
                    downDescription = stringResource(R.string.key_channel_down),
                    onDown = key(RemoteKey.CHANNEL_DOWN),
                    haptic = haptic,
                )
            } else {
                Spacer(Modifier.width(RemoteButtonSize))
            }
        }

        // Silencio y reproducción
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (has(Capability.VOLUME)) {
                RemoteButton(
                    icon = Icons.AutoMirrored.Filled.VolumeOff,
                    contentDescription = stringResource(R.string.key_mute),
                    onPress = key(RemoteKey.MUTE),
                    haptic = haptic,
                )
            }
            if (has(Capability.MEDIA)) {
                RemoteButton(
                    icon = Icons.Filled.PlayArrow,
                    contentDescription = stringResource(R.string.key_play_pause),
                    onPress = key(RemoteKey.PLAY_PAUSE),
                    haptic = haptic,
                )
            }
        }

        // Fila inferior: teclado, touchpad, apps
        Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
            if (has(Capability.KEYBOARD)) {
                RemoteButton(
                    icon = Icons.Filled.Keyboard,
                    contentDescription = stringResource(R.string.key_keyboard),
                    onPress = { actions.onKeyboard() },
                    haptic = haptic,
                )
            }
            if (has(Capability.TOUCHPAD)) {
                RemoteButton(
                    icon = if (touchpadMode) Icons.Filled.Gamepad else Icons.Filled.TouchApp,
                    contentDescription = stringResource(
                        if (touchpadMode) R.string.key_dpad_mode else R.string.key_touchpad,
                    ),
                    onPress = { actions.onToggleTouchpad() },
                    haptic = haptic,
                )
            }
            if (has(Capability.APPS)) {
                RemoteButton(
                    icon = Icons.Filled.Apps,
                    contentDescription = stringResource(R.string.key_apps),
                    onPress = { actions.onApps() },
                    haptic = haptic,
                )
            }
        }
    }
}

@Composable
private fun RockerColumn(
    label: String,
    upIcon: ImageVector,
    upDescription: String,
    onUp: (Boolean) -> Unit,
    downIcon: ImageVector,
    downDescription: String,
    onDown: (Boolean) -> Unit,
    haptic: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 4.dp),
    ) {
        RemoteButton(upIcon, upDescription, onUp, haptic, repeatable = true)
        // La etiqueta es visual; cada botón ya tiene su descripción.
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        RemoteButton(downIcon, downDescription, onDown, haptic, repeatable = true)
    }
}

@Composable
private fun DPad(onKey: (RemoteKey, Boolean) -> Unit, haptic: () -> Unit) {
    Box(
        modifier = Modifier
            .size(192.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        RemoteButton(
            icon = Icons.Filled.KeyboardArrowUp,
            contentDescription = stringResource(R.string.key_up),
            onPress = { onKey(RemoteKey.UP, it) },
            haptic = haptic,
            repeatable = true,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 4.dp),
        )
        RemoteButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
            contentDescription = stringResource(R.string.key_left),
            onPress = { onKey(RemoteKey.LEFT, it) },
            haptic = haptic,
            repeatable = true,
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 4.dp),
        )
        RemoteButton(
            icon = Icons.Filled.Circle,
            contentDescription = stringResource(R.string.key_ok),
            onPress = { onKey(RemoteKey.OK, it) },
            haptic = haptic,
            emphasized = true,
            size = 72.dp,
            modifier = Modifier.align(Alignment.Center),
        )
        RemoteButton(
            icon = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = stringResource(R.string.key_right),
            onPress = { onKey(RemoteKey.RIGHT, it) },
            haptic = haptic,
            repeatable = true,
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp),
        )
        RemoteButton(
            icon = Icons.Filled.KeyboardArrowDown,
            contentDescription = stringResource(R.string.key_down),
            onPress = { onKey(RemoteKey.DOWN, it) },
            haptic = haptic,
            repeatable = true,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp),
        )
    }
}

/**
 * Touchpad: deslizar mueve el puntero, tocar hace clic. Para TalkBack expone una acción
 * de clic. Con drivers de red (hitos 3–4) habrá que agrupar movimientos para no saturar la tele.
 */
@Composable
private fun Touchpad(onMove: (Float, Float) -> Unit, onTap: () -> Unit) {
    val description = stringResource(R.string.touchpad_description)
    val currentOnMove by rememberUpdatedState(onMove)
    val currentOnTap by rememberUpdatedState(onTap)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(192.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .semantics {
                contentDescription = description
                onClick {
                    currentOnTap()
                    true
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, drag ->
                    change.consume()
                    currentOnMove(drag.x, drag.y)
                }
            }
            .pointerInput(Unit) { detectTapGestures(onTap = { currentOnTap() }) },
    ) {
        Text(
            stringResource(R.string.touchpad_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp),
        )
    }
}
