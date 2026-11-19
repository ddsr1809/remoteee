package app.remoto.control.ui.help

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import app.remoto.control.ui.theme.RemotoTheme

/** Ayuda: lista de temas o un tema concreto. Las guías por marca llegan en el hito 4. */
@Composable
fun HelpScreen(
    topic: HelpTopic?,
    onBack: () -> Unit,
    onOpenTopic: (HelpTopic) -> Unit,
) {
    Scaffold(
        topBar = {
            RemotoTopBar(
                title = stringResource(topic?.text()?.title ?: R.string.help_title),
                onBack = onBack,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
        ) {
            if (topic != null) {
                Text(
                    stringResource(topic.text().body),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp),
                )
                HorizontalDivider()
                Text(
                    stringResource(R.string.help_other_topics),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
                )
            }
            TopicList(exclude = topic, onOpenTopic = onOpenTopic)
        }
    }
}

@Composable
private fun TopicList(exclude: HelpTopic?, onOpenTopic: (HelpTopic) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        HelpTopic.entries.filter { it != exclude }.forEach { t ->
            ListItem(
                headlineContent = { Text(stringResource(t.text().title)) },
                modifier = Modifier.clickable { onOpenTopic(t) },
            )
        }
    }
}

@Preview(name = "Ayuda — claro", locale = "es")
@Preview(name = "Help — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun HelpPreview() {
    RemotoTheme(dynamicColor = false) {
        HelpScreen(topic = HelpTopic.CONNECTION_TIMEOUT, onBack = {}, onOpenTopic = {})
    }
}
