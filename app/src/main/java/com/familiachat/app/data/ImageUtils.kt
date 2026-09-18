package com.familiachat.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

private const val MAX_DIMENSION = 1280
private const val JPEG_QUALITY = 80

/** Lê uma imagem escolhida pelo usuário, redimensiona e comprime pra não pesar no armazenamento. */
fun compressImageFromUri(context: Context, uri: Uri): ByteArray? {
    val resolver = context.contentResolver

    val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

    var sampleSize = 1
    while (boundsOptions.outWidth / sampleSize > MAX_DIMENSION || boundsOptions.outHeight / sampleSize > MAX_DIMENSION) {
        sampleSize *= 2
    }

    val bitmap = resolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    } ?: return null

    val scaled = if (bitmap.width > MAX_DIMENSION || bitmap.height > MAX_DIMENSION) {
        val ratio = minOf(MAX_DIMENSION.toFloat() / bitmap.width, MAX_DIMENSION.toFloat() / bitmap.height)
        Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt(), (bitmap.height * ratio).toInt(), true)
    } else {
        bitmap
    }

    return ByteArrayOutputStream().use { output ->
        scaled.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
        output.toByteArray()
    }
}
