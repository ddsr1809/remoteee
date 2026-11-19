package app.remoto.control.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import app.remoto.control.R
import app.remoto.control.ui.ComingSoonFeature
import app.remoto.control.ui.theme.RemotoTheme

/** Primera apertura. El consentimiento de anuncios (UMP) se añade antes de esto en el hito 6. */
@Composable
fun WelcomeScreen(
    onDiscover: () -> Unit,
    onComingSoon: (ComingSoonFeature) -> Unit,
    onDemoReady: (String) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    LaunchedEffect(viewModel) { viewModel.demoReady.collect { onDemoReady(it) } }
    WelcomeContent(onDiscover = onDiscover, onComingSoon = onComingSoon, onTryDemo = viewModel::startDemo)
}

@Composable
private fun WelcomeContent(
    onDiscover: () -> Unit,
    onComingSoon: (ComingSoonFeature) -> Unit,
    onTryDemo: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(
            Icons.Filled.SettingsRemote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally),
        )
        Text(
            stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
        Text(
            stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StartOptions(onDiscover = onDiscover, onComingSoon = onComingSoon, onTryDemo = onTryDemo)
    }
}

@Preview(name = "Bienvenida — claro", locale = "es")
@Preview(name = "Welcome — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun WelcomePreview() {
    RemotoTheme(dynamicColor = false) {
        Surface { WelcomeContent(onDiscover = {}, onComingSoon = {}, onTryDemo = {}) }
    }
}
