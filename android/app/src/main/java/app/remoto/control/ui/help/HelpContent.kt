package app.remoto.control.ui.help

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.remoto.control.R
import app.remoto.control.core.driver.HelpTopic

/** Textos de cada tema de ayuda. El núcleo solo conoce el [HelpTopic]. */
data class HelpText(@param:StringRes val title: Int, @param:StringRes val body: Int)

fun HelpTopic.text(): HelpText = when (this) {
    HelpTopic.GETTING_STARTED -> HelpText(R.string.help_getting_started_title, R.string.help_getting_started_body)
    HelpTopic.DEMO_MODE -> HelpText(R.string.help_demo_mode_title, R.string.help_demo_mode_body)
    HelpTopic.CONNECTION_TIMEOUT -> HelpText(R.string.help_timeout_title, R.string.help_timeout_body)
    HelpTopic.PAIRING_REJECTED -> HelpText(R.string.help_pairing_rejected_title, R.string.help_pairing_rejected_body)
    HelpTopic.LOCAL_NETWORK_PERMISSION ->
        HelpText(R.string.help_local_network_title, R.string.help_local_network_body)
    HelpTopic.NO_WIFI -> HelpText(R.string.help_no_wifi_title, R.string.help_no_wifi_body)
    HelpTopic.DEVICE_UNREACHABLE -> HelpText(R.string.help_unreachable_title, R.string.help_unreachable_body)
    HelpTopic.TV_NOT_FOUND -> HelpText(R.string.help_tv_not_found_title, R.string.help_tv_not_found_body)
}

/**
 * Lo que ve el usuario cuando falla la conexión: el tema de ayuda completo del error,
 * con "Reintentar" y un enlace a la ayuda general.
 */
@Composable
fun ConnectionErrorContent(
    topic: HelpTopic,
    onRetry: () -> Unit,
    onMoreHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = topic.text()
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            Icons.Filled.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Text(stringResource(R.string.error_heading), style = MaterialTheme.typography.labelLarge)
        Text(stringResource(text.title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(text.body), style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_retry))
        }
        OutlinedButton(onClick = onMoreHelp, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_more_help))
        }
    }
}
