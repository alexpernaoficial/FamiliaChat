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
fun PinLockScreen(prefs: LocalPrefs, onUnlocked: () -> Unit) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Text("Digite o PIN", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(32.dp))

        PinDotsField(
            value = pin,
            isError = error,
            onValueChange = { new ->
                pin = new
                error = false
                if (new.length == 4) {
                    if (prefs.checkPin(new)) {
                        onUnlocked()
                    } else {
                        error = true
                        pin = ""
                    }
                }
            }
        )

        if (error) {
            Spacer(Modifier.height(16.dp))
            Text("PIN incorreto", color = MaterialTheme.colorScheme.error)
        }
    }
}
