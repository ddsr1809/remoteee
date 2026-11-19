package app.remoto.control.ui.pairing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.remoto.control.R
import app.remoto.control.core.driver.PairingType

private const val MIN_CODE_LENGTH = 4
private const val MAX_CODE_LENGTH = 8

/**
 * Emparejamiento: explica qué verá el usuario en su tele y, si hace falta, pide el código.
 * Lo usan todos los drivers a través de [PairingType]; no sabe de marcas.
 */
@Composable
fun PairingContent(
    type: PairingType,
    onSubmit: (String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.pairing_title), style = MaterialTheme.typography.titleLarge)
        when (type) {
            PairingType.CODE_ON_TV -> CodeForm(onSubmit)
            PairingType.CONFIRM_ON_TV -> {
                Text(stringResource(R.string.pairing_confirm_instructions), style = MaterialTheme.typography.bodyLarge)
                Button(onClick = { onSubmit(null) }, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.pairing_confirm_done))
                }
            }
            PairingType.NONE -> Unit
        }
    }
}

@Composable
private fun CodeForm(onSubmit: (String?) -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val valid = code.length >= MIN_CODE_LENGTH
    Text(stringResource(R.string.pairing_code_instructions), style = MaterialTheme.typography.bodyLarge)
    OutlinedTextField(
        value = code,
        onValueChange = { input -> code = input.filter { it.isLetterOrDigit() }.take(MAX_CODE_LENGTH) },
        label = { Text(stringResource(R.string.pairing_code_label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (valid) onSubmit(code) }),
        modifier = Modifier.fillMaxWidth(),
    )
    Button(onClick = { onSubmit(code) }, enabled = valid, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.pairing_submit))
    }
}
