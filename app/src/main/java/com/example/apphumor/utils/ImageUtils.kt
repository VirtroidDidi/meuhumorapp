package com.example.apphumor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {
    private const val MAX_DIMENSION = 400 // Tamanho máximo (leve)
    private const val COMPRESSION_QUALITY = 70

    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            if (originalBitmap == null) return null

            val resizedBitmap = reduceBitmap(originalBitmap)
            bitmapToBase64(resizedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun base64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) return null
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) { null }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, COMPRESSION_QUALITY, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun reduceBitmap(original: Bitmap): Bitmap {
        val width = original.width
        val height = original.height
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return original

        val ratio: Float = width.toFloat() / height.toFloat()
        var newWidth = MAX_DIMENSION
        var newHeight = MAX_DIMENSION
        if (width > height) newHeight = (newWidth / ratio).toInt() else newWidth = (newHeight * ratio).toInt()

        return Bitmap.createScaledBitmap(original, newWidth, newHeight, true)
    }
}