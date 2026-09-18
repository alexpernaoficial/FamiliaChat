package com.familiachat.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.familiachat.app.data.FamilyRepository
import com.familiachat.app.data.LocalPrefs
import kotlinx.coroutines.launch

private enum class PairingMode { ESCOLHA, CRIAR, ENTRAR }

@Composable
fun PairingScreen(
    repository: FamilyRepository,
    prefs: LocalPrefs,
    onPaired: () -> Unit
) {
    var mode by remember { mutableStateOf(PairingMode.ESCOLHA) }
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(8.dp))
        Text("Família Chat", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "só entre vocês",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))

        when (mode) {
            PairingMode.ESCOLHA -> {
                Text("Este celular é do responsável ou da criança?", textAlign = TextAlign.Center)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = { mode = PairingMode.CRIAR },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Sou o responsável: criar família")
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { mode = PairingMode.ENTRAR },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Recebi um código: entrar na família")
                }
            }

            PairingMode.CRIAR -> {
                if (generatedCode == null) {
                    Text("Digite seu nome para criar a família:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Seu nome") },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        enabled = name.isNotBlank() && !loading,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        onClick = {
                            error = null
                            loading = true
                            scope.launch {
                                try {
                                    val result = repository.createFamily(name.trim())
                                    generatedCode = result
                                    prefs.familyId = result
                                    prefs.displayName = name.trim()
                                } catch (e: Exception) {
                                    error = "Erro ao criar família: ${e.message}"
                                } finally {
                                    loading = false
                                }
                            }
                        }
                    ) { Text(if (loading) "Criando..." else "Criar família") }
                } else {
                    Text("🎉 Família criada!", style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            generatedCode ?: "",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Digite esse código no celular do seu filho para vincular os dois aparelhos.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = onPaired,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Continuar")
                    }
                }
            }

            PairingMode.ENTRAR -> {
                Text("Digite seu nome e o código de 6 dígitos recebido:")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Seu nome") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = code,
                    onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                    label = { Text("Código da família") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    enabled = name.isNotBlank() && code.length == 6 && !loading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    onClick = {
                        error = null
                        loading = true
                        scope.launch {
                            try {
                                val ok = repository.joinFamily(code, name.trim())
                                if (ok) {
                                    prefs.familyId = code
                                    prefs.displayName = name.trim()
                                    onPaired()
                                } else {
                                    error = "Código não encontrado. Confira com quem te passou o código."
                                }
                            } catch (e: Exception) {
                                error = "Erro ao entrar na família: ${e.message}"
                            } finally {
                                loading = false
                            }
                        }
                    }
                ) { Text(if (loading) "Entrando..." else "Entrar na família") }
            }
        }

        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        }
    }
}
