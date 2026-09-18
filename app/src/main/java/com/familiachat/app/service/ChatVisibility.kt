package com.familiachat.app.service

/** Marcado como true pela ChatScreen enquanto ela está visível, para o FcmService não notificar à toa. */
object ChatVisibility {
    @Volatile
    var isVisible: Boolean = false
}
