package app.remoto.control.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.remoto.control.R
import app.remoto.control.ui.ComingSoonFeature

/**
 * Las tres formas de empezar. "Probar sin tele" siempre está visible: la usan los
 * revisores de Google Play (sección 7.6).
 */
@Composable
fun StartOptions(
    onComingSoon: (ComingSoonFeature) -> Unit,
    onTryDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OptionCard(
            icon = Icons.Filled.Wifi,
            title = stringResource(R.string.start_option_wifi),
            description = stringResource(R.string.start_option_wifi_desc),
            onClick = { onComingSoon(ComingSoonFeature.WIFI) },
        )
        OptionCard(
            icon = Icons.Filled.SettingsRemote,
            title = stringResource(R.string.start_option_ir),
            description = stringResource(R.string.start_option_ir_desc),
            onClick = { onComingSoon(ComingSoonFeature.INFRARED) },
        )
        OptionCard(
            icon = Icons.Filled.Tv,
            title = stringResource(R.string.start_option_demo),
            description = stringResource(R.string.start_option_demo_desc),
            onClick = onTryDemo,
            highlighted = true,
        )
    }
}

@Composable
private fun OptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp),
        colors = if (highlighted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // El texto de la tarjeta ya describe la acción; el ícono es decorativo.
            Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
