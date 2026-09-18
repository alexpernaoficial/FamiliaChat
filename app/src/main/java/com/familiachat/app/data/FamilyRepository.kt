package com.familiachat.app.data

import android.net.Uri
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.io.File
import java.util.UUID

/**
 * Toda a comunicação com o Firebase (Auth anônimo + Firestore + Storage) fica isolada aqui.
 * Não existe busca de usuários nem lista de contatos: só entra na "família"
 * quem digitar o código de 6 dígitos gerado por quem criou o grupo.
 */
class FamilyRepository {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()

    val currentUid: String
        get() = auth.currentUser?.uid ?: error("Usuário não autenticado ainda")

    suspend fun ensureSignedIn(): String {
        auth.currentUser?.let { return it.uid }
        val result = auth.signInAnonymously().await()
        return result.user?.uid ?: error("Falha ao autenticar")
    }

    /** Cria uma nova família com um código de 6 dígitos único e retorna o código (== familyId). */
    suspend fun createFamily(displayName: String): String {
        val uid = ensureSignedIn()
        val familiesRef = db.collection("families")

        repeat(5) {
            val code = (100000..999999).random().toString()
            val docRef = familiesRef.document(code)
            val existing = docRef.get().await()
            if (!existing.exists()) {
                val data = mapOf(
                    "code" to code,
                    "members" to listOf(uid),
                    "memberNames" to mapOf(uid to displayName),
                    "createdAt" to FieldValue.serverTimestamp()
                )
                docRef.set(data).await()
                return code
            }
        }
        error("Não foi possível gerar um código único, tente novamente")
    }

    /** Entra em uma família existente usando o código gerado no outro celular. */
    suspend fun joinFamily(code: String, displayName: String): Boolean {
        val uid = ensureSignedIn()
        val docRef = db.collection("families").document(code)
        val snapshot = docRef.get().await()
        if (!snapshot.exists()) return false

        docRef.update(
            mapOf(
                "members" to FieldValue.arrayUnion(uid),
                "memberNames.$uid" to displayName
            )
        ).await()
        return true
    }

    fun sendMessage(familyId: String, senderName: String, text: String) {
        val message = mapOf(
            "senderId" to currentUid,
            "senderName" to senderName,
            "text" to text,
            "type" to MessageType.TEXT.name,
            "timestamp" to FieldValue.serverTimestamp()
        )
        messagesRef(familyId).add(message)
    }

    /** Envia uma foto: sobe pro Storage e depois cria a mensagem apontando pra ela. */
    suspend fun sendImageMessage(familyId: String, senderName: String, imageBytes: ByteArray) {
        val path = "families/$familyId/media/${UUID.randomUUID()}.jpg"
        val ref = storage.reference.child(path)
        ref.putBytes(imageBytes).await()
        val url = ref.downloadUrl.await().toString()

        val message = mapOf(
            "senderId" to currentUid,
            "senderName" to senderName,
            "text" to "",
            "type" to MessageType.IMAGE.name,
            "mediaUrl" to url,
            "mediaPath" to path,
            "timestamp" to FieldValue.serverTimestamp()
        )
        messagesRef(familyId).add(message).await()
    }

    /** Envia um áudio gravado: sobe pro Storage e depois cria a mensagem apontando pra ele. */
    suspend fun sendAudioMessage(familyId: String, senderName: String, audioFile: File, durationSeconds: Int) {
        val path = "families/$familyId/media/${UUID.randomUUID()}.m4a"
        val ref = storage.reference.child(path)
        ref.putFile(Uri.fromFile(audioFile)).await()
        val url = ref.downloadUrl.await().toString()

        val message = mapOf(
            "senderId" to currentUid,
            "senderName" to senderName,
            "text" to "",
            "type" to MessageType.AUDIO.name,
            "mediaUrl" to url,
            "mediaPath" to path,
            "durationSeconds" to durationSeconds,
            "timestamp" to FieldValue.serverTimestamp()
        )
        messagesRef(familyId).add(message).await()
    }

    /** Salva o token de notificação (FCM) deste aparelho, usado pela Cloud Function pra saber pra quem mandar push. */
    fun saveFcmToken(familyId: String, token: String) {
        db.collection("families").document(familyId)
            .update("memberTokens.$currentUid", token)
    }

    fun listenMessages(
        familyId: String,
        onChange: (List<Message>) -> Unit
    ): ListenerRegistration {
        return messagesRef(familyId)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot == null) return@addSnapshotListener
                val messages = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(Message::class.java)?.copy(id = doc.id)
                }
                onChange(messages)
            }
    }

    private fun messagesRef(familyId: String) =
        db.collection("families").document(familyId).collection("messages")
}
