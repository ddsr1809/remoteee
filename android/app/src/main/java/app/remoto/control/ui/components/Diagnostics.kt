package app.remoto.control.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import app.remoto.control.R
import app.remoto.control.core.diagnostics.DiagnosticsLog
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class DiagnosticsViewModel @Inject constructor(
    private val log: DiagnosticsLog,
) : ViewModel() {
    /** Cabecera con la versión de la app y del teléfono: ayuda a reproducir el problema. */
    fun export(appVersion: String): String = log.export(
        header = listOf(
            "Remoto $appVersion",
            "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            "${Build.MANUFACTURER} ${Build.MODEL}",
        ),
    )
}

/**
 * Devuelve una acción que copia el registro de diagnóstico al portapapeles. No lo envía a
 * ningún lado: el usuario decide si lo pega en un mensaje.
 */
@Composable
fun rememberCopyDiagnostics(viewModel: DiagnosticsViewModel = hiltViewModel()): () -> Unit {
    val context = LocalContext.current
    return remember(context, viewModel) {
        { copyToClipboard(context, viewModel.export(appVersion(context))) }
    }
}

private fun appVersion(context: Context): String =
    runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return
    clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.diagnostics_clip_label), text))
    // Desde Android 13 el sistema ya muestra su propio aviso al copiar.
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, R.string.diagnostics_copied, Toast.LENGTH_SHORT).show()
    }
}
