package com.example.apphumor.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

object ImageUtils {

    // TODO: [PERFORMANCE] Otimização de Imagem
    // Como estamos usando Base64 (MVP), a compressão aqui deve ser agressiva.
    // Quality = 50 é um compromisso entre legibilidade e tamanho da String final.
    // Em produção com Storage, poderíamos subir a qualidade para 80+.
    fun uriToBase64(context: Context, uri: Uri): String? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            if (bitmap != null) {
                // Redimensiona se for muito grande (opcional, mas recomendado para Base64)
                val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)

                val outputStream = ByteArrayOutputStream()
                // Comprime para JPEG com 50% de qualidade
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
                val byteArray = outputStream.toByteArray()

                Base64.encodeToString(byteArray, Base64.DEFAULT)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}