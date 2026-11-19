package app.remoto.control.ui.discover

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.remoto.control.R
import app.remoto.control.ui.theme.RemotoTheme

/**
 * Explica el permiso de red local de Android 17 antes de pedirlo.
 *
 * **Hoy no se muestra nunca**: con `targetSdk` 36 el permiso no aplica (ver
 * `core/net/LocalNetworkAccess.kt`). Cuando se suba a `targetSdk` 37, el botón principal
 * debe lanzar la petición con `rememberLauncherForActivityResult(RequestPermission())`;
 * por ahora abre los ajustes de la app, que sirve también si el usuario ya lo negó.
 */
@Composable
fun LocalNetworkPermissionContent(onTryAgain: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            Icons.Filled.Devices,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp),
        )
        Text(stringResource(R.string.help_local_network_title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(R.string.local_network_body), style = MaterialTheme.typography.bodyLarge)
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .setData(Uri.fromParts("package", context.packageName, null))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { context.startActivity(intent) }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.local_network_open_settings))
        }
        OutlinedButton(onClick = onTryAgain, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.discover_search_again))
        }
    }
}

@Preview(name = "Permiso de red local — claro", locale = "es")
@Preview(name = "Local network permission — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LocalNetworkPermissionPreview() {
    RemotoTheme(dynamicColor = false) {
        Surface { LocalNetworkPermissionContent(onTryAgain = {}, modifier = Modifier.padding(16.dp)) }
    }
}
