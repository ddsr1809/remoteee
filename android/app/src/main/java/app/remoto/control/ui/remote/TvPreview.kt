package app.remoto.control.ui.remote

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.remoto.control.R
import app.remoto.control.core.driver.RemoteKey
import app.remoto.control.core.driver.TvApp
import app.remoto.control.core.driver.TvPreviewState
import kotlinx.coroutines.delay

// La "pantalla" de la tele es oscura en ambos temas, como una tele de verdad.
private val ScreenBackground = Color(0xFF0B0F14)
private val ScreenFrame = Color(0xFF2A2F36)
private val TileColor = Color(0xFF1E2A36)
private val FocusColor = Color(0xFF7FE3CC)
private val ScreenText = Color(0xFFE8EEF2)
private val ScreenDim = Color(0xFF8A96A3)

private const val KEY_FLASH_MS = 900L

/** Tele simulada del modo demostración. Cada tecla produce una reacción visible. */
@Composable
fun TvPreview(preview: TvPreviewState, modifier: Modifier = Modifier) {
    // Muestra durante un momento la última tecla recibida (cambia con cada pulsación).
    var flashKey by remember { mutableStateOf<RemoteKey?>(null) }
    LaunchedEffect(preview.keyCount) {
        if (preview.keyCount > 0) {
            flashKey = preview.lastKey
            delay(KEY_FLASH_MS)
            flashKey = null
        }
    }

    val description = previewDescription(preview)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(RoundedCornerShape(12.dp))
            .background(ScreenBackground)
            .border(4.dp, ScreenFrame, RoundedCornerShape(12.dp))
            .clearAndSetSemantics { contentDescription = description },
    ) {
        when {
            !preview.poweredOn -> CenterText(stringResource(R.string.tv_off), ScreenDim)
            preview.pairingCode != null -> PairingCodeScreen(preview.pairingCode)
            preview.openApp != null -> AppScreen(preview.openApp, preview.playing)
            else -> HomeGrid(preview)
        }

        if (preview.poweredOn && preview.pairingCode == null) {
            Overlays(preview, flashKey)
        }
        flashKey?.let { key ->
            Text(
                text = stringResource(R.string.tv_key_received, stringResource(keyLabelRes(key))),
                color = ScreenBackground,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(FocusColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun CenterText(text: String, color: Color) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, color = color, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun PairingCodeScreen(code: String) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.tv_pairing_code), color = ScreenDim, style = MaterialTheme.typography.labelLarge)
        Text(code, color = ScreenText, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp)
    }
}

@Composable
private fun AppScreen(app: TvApp, playing: Boolean) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(demoAppName(app), color = ScreenText, style = MaterialTheme.typography.headlineSmall)
        Icon(
            if (playing) Icons.Filled.PlayArrow else Icons.Filled.Pause,
            contentDescription = null,
            tint = FocusColor,
            modifier = Modifier.size(40.dp),
        )
        Text(
            stringResource(if (playing) R.string.tv_playing else R.string.tv_paused),
            color = ScreenDim,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun HomeGrid(preview: TvPreviewState) {
    val columns = 4
    Column(
        modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, top = 36.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        preview.tiles.chunked(columns).forEachIndexed { row, tiles ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                tiles.forEachIndexed { col, app ->
                    val focused = row * columns + col == preview.focusedTile
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(TileColor)
                            .then(
                                if (focused) Modifier.border(2.dp, FocusColor, RoundedCornerShape(6.dp)) else Modifier,
                            ),
                    ) {
                        Text(
                            demoAppName(app),
                            color = if (focused) FocusColor else ScreenText,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Overlays(preview: TvPreviewState, flashKey: RemoteKey?) {
    Box(Modifier.fillMaxSize()) {
        Text(
            stringResource(R.string.tv_channel, preview.channel),
            color = ScreenText,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.align(Alignment.TopEnd).padding(10.dp),
        )
        if (preview.muted) {
            Icon(
                Icons.AutoMirrored.Filled.VolumeOff,
                contentDescription = null,
                tint = ScreenText,
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp).size(20.dp),
            )
        }
        if (flashKey == RemoteKey.VOLUME_UP || flashKey == RemoteKey.VOLUME_DOWN || flashKey == RemoteKey.MUTE) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(0.6f).padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    stringResource(R.string.tv_volume, preview.volume),
                    color = ScreenText,
                    style = MaterialTheme.typography.labelSmall,
                )
                LinearProgressIndicator(
                    progress = { if (preview.muted) 0f else preview.volume / 100f },
                    color = FocusColor,
                    trackColor = TileColor,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (preview.typedText.isNotEmpty()) {
            Text(
                preview.typedText,
                color = ScreenText,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(TileColor)
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        if (preview.pointerVisible) {
            BoxWithConstraints(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .offset(x = maxWidth * preview.pointerX - 6.dp, y = maxHeight * preview.pointerY - 6.dp)
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, ScreenBackground, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun previewDescription(preview: TvPreviewState): String = when {
    !preview.poweredOn -> stringResource(R.string.tv_description_off)
    preview.pairingCode != null -> stringResource(R.string.tv_description_pairing, preview.pairingCode)
    else -> stringResource(
        R.string.tv_description_on,
        preview.openApp?.let { demoAppName(it) } ?: stringResource(R.string.tv_home),
        if (preview.muted) 0 else preview.volume,
        preview.channel,
    )
}

/** Las apps de la tele simulada tienen nombre traducido; las de teles reales usan el suyo. */
@Composable
fun demoAppName(app: TvApp): String {
    val res = when (app.id) {
        "movies" -> R.string.demo_app_movies
        "series" -> R.string.demo_app_series
        "music" -> R.string.demo_app_music
        "news" -> R.string.demo_app_news
        "sports" -> R.string.demo_app_sports
        "kids" -> R.string.demo_app_kids
        "photos" -> R.string.demo_app_photos
        "games" -> R.string.demo_app_games
        else -> null
    }
    return res?.let { stringResource(it) } ?: app.name
}

fun keyLabelRes(key: RemoteKey): Int = when (key) {
    RemoteKey.UP -> R.string.key_up
    RemoteKey.DOWN -> R.string.key_down
    RemoteKey.LEFT -> R.string.key_left
    RemoteKey.RIGHT -> R.string.key_right
    RemoteKey.OK -> R.string.key_ok
    RemoteKey.BACK -> R.string.key_back
    RemoteKey.HOME -> R.string.key_home
    RemoteKey.VOLUME_UP -> R.string.key_volume_up
    RemoteKey.VOLUME_DOWN -> R.string.key_volume_down
    RemoteKey.MUTE -> R.string.key_mute
    RemoteKey.CHANNEL_UP -> R.string.key_channel_up
    RemoteKey.CHANNEL_DOWN -> R.string.key_channel_down
    RemoteKey.PLAY_PAUSE -> R.string.key_play_pause
    RemoteKey.POWER -> R.string.key_power
}
