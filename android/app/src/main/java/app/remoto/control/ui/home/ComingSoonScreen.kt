package app.remoto.control.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.remoto.control.R
import app.remoto.control.ui.ComingSoonFeature
import app.remoto.control.ui.components.RemotoTopBar

/** Pantalla provisional para Wi-Fi (hito 2) e infrarrojo (hito 5). */
@Composable
fun ComingSoonScreen(
    feature: ComingSoonFeature,
    onBack: () -> Unit,
    onDemoReady: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.demoReady.collect { onDemoReady(it) } }
    val (icon, title, body) = when (feature) {
        ComingSoonFeature.WIFI -> Triple(Icons.Filled.Wifi, R.string.start_option_wifi, R.string.coming_soon_wifi_body)
        ComingSoonFeature.INFRARED ->
            Triple(Icons.Filled.SettingsRemote, R.string.start_option_ir, R.string.coming_soon_ir_body)
    }
    Scaffold(topBar = { RemotoTopBar(title = stringResource(title), onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Text(stringResource(R.string.coming_soon_title), style = MaterialTheme.typography.headlineSmall)
            Text(stringResource(body), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = viewModel::startDemo, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.start_option_demo))
            }
        }
    }
}
