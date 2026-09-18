package com.familiachat.app.data

import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

enum class MessageType { TEXT, IMAGE, AUDIO }

data class Message(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val type: String = "TEXT",
    val mediaUrl: String? = null,
    val mediaPath: String? = null,
    val durationSeconds: Int = 0,
    val expired: Boolean = false,
    @ServerTimestamp
    @get:PropertyName("timestamp")
    @set:PropertyName("timestamp")
    var timestamp: Date? = null
) {
    val messageType: MessageType
        get() = try { MessageType.valueOf(type) } catch (e: IllegalArgumentException) { MessageType.TEXT }
}
