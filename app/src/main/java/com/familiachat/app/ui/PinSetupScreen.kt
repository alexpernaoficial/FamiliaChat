package com.familiachat.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.familiachat.app.data.LocalPrefs
import com.familiachat.app.ui.components.PinDotsField

@Composable
fun PinSetupScreen(prefs: LocalPrefs, onPinSet: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var confirming by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun tryFinish(finalConfirm: String) {
        if (pin != finalConfirm) {
            error = "Os PINs não são iguais, tente de novo"
            confirmPin = ""
            confirming = false
            pin = ""
        } else {
            prefs.setPin(pin)
            onPinSet()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🔒", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text(
            if (!confirming) "Crie um PIN de 4 dígitos" else "Confirme o PIN",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Esse PIN vai ser pedido toda vez que o app for aberto neste celular.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        if (!confirming) {
            PinDotsField(
                value = pin,
                onValueChange = {
                    pin = it
                    if (it.length == 4) confirming = true
                }
            )
        } else {
            PinDotsField(
                value = confirmPin,
                isError = error != null,
                onValueChange = {
                    confirmPin = it
                    error = null
                    if (it.length == 4) tryFinish(it)
                }
            )
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}
