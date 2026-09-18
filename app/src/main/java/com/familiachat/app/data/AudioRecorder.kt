package com.familiachat.app.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import java.io.File

/** Grava áudio (voz) num arquivo temporário local, usando o microfone do aparelho. */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null
    private var outputFile: File? = null
    private var startTime: Long = 0

    fun start(): File {
        val file = File(context.cacheDir, "audio_${System.currentTimeMillis()}.m4a")
        outputFile = file

        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }

        mediaRecorder.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(48000)
            setAudioSamplingRate(44100)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        recorder = mediaRecorder
        startTime = System.currentTimeMillis()
        return file
    }

    /** Para a gravação e retorna o arquivo + duração em segundos. Retorna null se a gravação foi curta demais (toque acidental). */
    fun stop(): Pair<File, Int>? {
        val durationSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            // Gravação muito curta ou sem áudio capturado; descarta.
            recorder?.release()
            recorder = null
            outputFile?.delete()
            return null
        }
        recorder = null

        val file = outputFile ?: return null
        if (durationSeconds < 1) {
            file.delete()
            return null
        }
        return file to durationSeconds
    }

    fun cancel() {
        try {
            recorder?.apply {
                stop()
                release()
            }
        } catch (e: RuntimeException) {
            recorder?.release()
        }
        recorder = null
        outputFile?.delete()
    }
}
