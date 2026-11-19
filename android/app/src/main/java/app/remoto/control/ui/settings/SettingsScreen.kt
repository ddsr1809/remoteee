package app.remoto.control.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.remoto.control.R
import app.remoto.control.core.storage.AppSettings
import app.remoto.control.core.storage.ThemeMode
import app.remoto.control.ui.components.RemotoTopBar
import app.remoto.control.ui.theme.RemotoTheme

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val version = remember(context) {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull()
    }
    SettingsContent(
        state = state,
        version = version.orEmpty(),
        onBack = onBack,
        onHelp = onHelp,
        onHaptics = viewModel::setHaptics,
        onTheme = viewModel::setTheme,
    )
}

@Composable
private fun SettingsContent(
    state: AppSettings,
    version: String,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    onHaptics: (Boolean) -> Unit,
    onTheme: (ThemeMode) -> Unit,
) {
    Scaffold(topBar = { RemotoTopBar(title = stringResource(R.string.settings_title), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_haptics)) },
                supportingContent = { Text(stringResource(R.string.settings_haptics_desc)) },
                trailingContent = { Switch(checked = state.hapticsEnabled, onCheckedChange = null) },
                modifier = Modifier.toggleable(
                    value = state.hapticsEnabled,
                    role = Role.Switch,
                    onValueChange = onHaptics,
                ),
            )
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_theme))
            Column(Modifier.selectableGroup()) {
                listOf(
                    ThemeMode.SYSTEM to R.string.theme_system,
                    ThemeMode.LIGHT to R.string.theme_light,
                    ThemeMode.DARK to R.string.theme_dark,
                ).forEach { (mode, label) ->
                    ListItem(
                        headlineContent = { Text(stringResource(label)) },
                        leadingContent = { RadioButton(selected = state.themeMode == mode, onClick = null) },
                        modifier = Modifier.selectable(
                            selected = state.themeMode == mode,
                            role = Role.RadioButton,
                            onClick = { onTheme(mode) },
                        ),
                    )
                }
            }
            HorizontalDivider()

            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_language)) },
                supportingContent = { Text(stringResource(R.string.settings_language_desc)) },
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.help_title)) },
                modifier = Modifier.clickable(onClick = onHelp),
            )
            HorizontalDivider()

            SectionTitle(stringResource(R.string.settings_about))
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version, version)) },
                supportingContent = { Text(stringResource(R.string.settings_disclaimer)) },
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp),
    )
}

@Preview(name = "Ajustes — claro", locale = "es", heightDp = 800)
@Preview(name = "Settings — dark", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES, heightDp = 800)
@Composable
private fun SettingsPreview() {
    RemotoTheme(dynamicColor = false) {
        SettingsContent(AppSettings(), "0.1.0", {}, {}, {}, {})
    }
}
