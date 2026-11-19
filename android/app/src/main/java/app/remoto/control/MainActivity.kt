package app.remoto.control

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import app.remoto.control.ui.RemotoRoot
import dagger.hilt.android.AndroidEntryPoint

/** Única Activity. Todo lo demás son pantallas Compose. */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { RemotoRoot() }
    }
}
