package app.remoto.control.ui.discover

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.remoto.control.R
import app.remoto.control.core.driver.HelpTopic
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.components.rememberCopyDiagnostics
import app.remoto.control.ui.help.text
import app.remoto.control.ui.theme.RemotoTheme

/** "No aparece mi tele": consejos, IP manual y registro de diagnóstico. */
@Composable
fun NotFoundScreen(
    onBack: () -> Unit,
    onManualAdd: () -> Unit,
) {
    NotFoundContent(onBack = onBack, onManualAdd = onManualAdd, onCopyDiagnostics = rememberCopyDiagnostics())
}

@Composable
private fun NotFoundContent(
    onBack: () -> Unit,
    onManualAdd: () -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    val topic = HelpTopic.TV_NOT_FOUND.text()
    Scaffold(topBar = { RemotoTopBar(title = stringResource(topic.title), onBack = onBack) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(stringResource(topic.body), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onManualAdd, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.manual_title))
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.discover_search_again))
            }
            HorizontalDivider()
            Text(stringResource(R.string.diagnostics_copy_desc), style = MaterialTheme.typography.bodyMedium)
            OutlinedButton(
                onClick = onCopyDiagnostics,
                modifier = Modifier.fillMaxWidth(),
                contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
            ) {
                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                Text(stringResource(R.string.diagnostics_copy))
            }
        }
    }
}

@Preview(name = "No aparece mi tele — claro", locale = "es", heightDp = 900)
@Preview(name = "TV not found — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, heightDp = 900)
@Composable
private fun NotFoundPreview() {
    RemotoTheme(dynamicColor = false) {
        NotFoundContent(onBack = {}, onManualAdd = {}, onCopyDiagnostics = {})
    }
}
