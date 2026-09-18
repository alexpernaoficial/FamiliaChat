package com.familiachat.app.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.CancellationException
import com.familiachat.app.data.AudioPlayer
import com.familiachat.app.data.AudioRecorder
import com.familiachat.app.data.FamilyRepository
import com.familiachat.app.data.LocalPrefs
import com.familiachat.app.data.Message
import com.familiachat.app.data.MessageType
import com.familiachat.app.data.compressImageFromUri
import com.familiachat.app.service.ChatVisibility
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(repository: FamilyRepository, prefs: LocalPrefs) {
    val familyId = prefs.familyId ?: return
    val myName = prefs.displayName ?: "Eu"
    val context = LocalContext.current
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var draft by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val audioRecorder = remember { AudioRecorder(context) }
    val audioPlayer = remember { AudioPlayer() }

    DisposableEffect(familyId) {
        ChatVisibility.isVisible = true
        val registration = repository.listenMessages(familyId) { messages = it }
        onDispose {
            ChatVisibility.isVisible = false
            registration.remove()
            audioPlayer.stop()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1)
    }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            scope.launch {
                val bytes = compressImageFromUri(context, uri)
                if (bytes != null) repository.sendImageMessage(familyId, myName, bytes)
            }
        }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Column {
                    Text("Família", fontWeight = FontWeight.Bold)
                    Text(
                        "chat privado",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            )
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                val isMine = message.senderId == repository.currentUid
                MessageBubble(message = message, isMine = isMine, audioPlayer = audioPlayer)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            }) {
                Text("📎", fontSize = 22.sp)
            }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Mensagem") },
                shape = RoundedCornerShape(28.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(Modifier.width(8.dp))

            val canSendText = draft.isNotBlank()
            val buttonColor = if (canSendText || isRecording) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(buttonColor)
                    .pointerInput(canSendText) {
                        if (canSendText) {
                            detectTapGestures(onTap = {
                                val text = draft.trim()
                                draft = ""
                                scope.launch { repository.sendMessage(familyId, myName, text) }
                            })
                        } else {
                            detectTapGestures(onPress = {
                                if (!hasRecordAudioPermission(context)) {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    return@detectTapGestures
                                }
                                isRecording = true
                                audioRecorder.start()
                                val released = try {
                                    awaitRelease()
                                    true
                                } catch (e: CancellationException) {
                                    false
                                }
                                isRecording = false
                                if (released) {
                                    audioRecorder.stop()?.let { (file, duration) ->
                                        scope.launch { repository.sendAudioMessage(familyId, myName, file, duration) }
                                    }
                                } else {
                                    audioRecorder.cancel()
                                }
                            })
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                if (canSendText) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("🎤", fontSize = 20.sp)
                }
            }
        }

        if (isRecording) {
            Text(
                "🎙️ Gravando... solte para enviar",
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun hasRecordAudioPermission(context: Context): Boolean {
    return androidx.core.content.ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun MessageBubble(message: Message, isMine: Boolean, audioPlayer: AudioPlayer) {
    val bubbleColor = if (isMine) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (isMine) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (isMine) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 16.dp)
    }
    val timeText = remember(message.timestamp) {
        message.timestamp?.let { SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(it) } ?: ""
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = bubbleColor,
            shape = shape,
            shadowElevation = 1.dp,
            modifier = Modifier.padding(2.dp).widthIn(max = 280.dp)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                if (!isMine) {
                    Text(
                        message.senderName,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(2.dp))
                }

                when {
                    message.expired -> Text(
                        "📎 mídia expirada (7 dias)",
                        color = textColor.copy(alpha = 0.7f),
                        fontStyle = FontStyle.Italic,
                        style = MaterialTheme.typography.bodyMedium
                    )

                    message.messageType == MessageType.IMAGE && message.mediaUrl != null -> {
                        AsyncImage(
                            model = message.mediaUrl,
                            contentDescription = "Foto",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(220.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }

                    message.messageType == MessageType.AUDIO && message.mediaUrl != null -> {
                        AudioMessageRow(
                            url = message.mediaUrl,
                            durationSeconds = message.durationSeconds,
                            textColor = textColor,
                            audioPlayer = audioPlayer
                        )
                    }

                    else -> Text(message.text, color = textColor, style = MaterialTheme.typography.bodyLarge)
                }

                if (timeText.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        timeText,
                        color = textColor.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun AudioMessageRow(url: String, durationSeconds: Int, textColor: androidx.compose.ui.graphics.Color, audioPlayer: AudioPlayer) {
    var playing by remember { mutableStateOf(false) }

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = {
            if (playing) {
                audioPlayer.stop()
                playing = false
            } else {
                playing = true
                audioPlayer.toggle(url) { playing = false }
            }
        }) {
            if (playing) {
                Text("⏹", fontSize = 20.sp, color = textColor)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = "Tocar áudio", tint = textColor)
            }
        }
        Text("🎵 áudio · ${durationSeconds}s", color = textColor, style = MaterialTheme.typography.bodyMedium)
    }
}
