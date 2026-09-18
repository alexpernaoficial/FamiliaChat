package com.familiachat.app.service

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.familiachat.app.FamiliaChatApp
import com.familiachat.app.MainActivity
import com.familiachat.app.data.FamilyRepository
import com.familiachat.app.data.LocalPrefs
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Recebe as notificações enviadas pela Cloud Function quando chega mensagem nova.
 * Usamos "data payload" (não "notification payload") para que este método seja
 * sempre chamado, mesmo com o app em primeiro plano — assim a gente decide na hora
 * se mostra a notificação ou não (não mostra se o chat já está aberto).
 */
class FcmService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        val prefs = LocalPrefs(applicationContext)
        val familyId = prefs.familyId ?: return
        FamilyRepository().saveFcmToken(familyId, token)
    }

    override fun onMessageReceived(message: RemoteMessage) {
        if (ChatVisibility.isVisible) return

        val senderName = message.data["senderName"] ?: "Família"
        val text = message.data["text"] ?: return

        val openAppIntent = Intent(this, MainActivity::class.java)
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, openAppIntent, flags)

        val notification = NotificationCompat.Builder(this, FamiliaChatApp.MESSAGE_CHANNEL_ID)
            .setContentTitle(senderName)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = getSystemService(android.app.NotificationManager::class.java)
        manager.notify(1, notification)
    }
}
