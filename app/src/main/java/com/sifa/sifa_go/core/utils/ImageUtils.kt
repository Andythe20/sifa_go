package com.sifa.sifa_go.core.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    /**
     * Comprime una imagen a menos de 5MB y corrige su rotación EXIF.
     * Esto asegura que la IA reciba la imagen derecha y con un peso optimizado.
     */
    fun compressImage(context: Context, sourceFile: File): String {
        val outputFolder = File(context.filesDir, "evidencia_multas")
        if (!outputFolder.exists()) outputFolder.mkdirs()

        val outputFile = File(outputFolder, "patente_${System.currentTimeMillis()}.jpg")

        try {
            // 1. Cargar el bitmap original
            val original = BitmapFactory.decodeFile(sourceFile.absolutePath)
                ?: run {
                    Log.w("ImageUtils", "No se pudo decodificar la imagen")
                    return sourceFile.absolutePath
                }

            // 2. Leer rotación EXIF (la causa de que la foto salga de costado)
            val exif = ExifInterface(sourceFile.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            }

            // 3. Crear el bitmap rotado correctamente
            val rotated = Bitmap.createBitmap(
                original, 0, 0, original.width, original.height, matrix, true
            )

            // 4. Guardar comprimido a JPEG (Calidad 80)
            FileOutputStream(outputFile).use { out ->
                rotated.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }

            Log.e("ImageUtils", "¡COMPRESIÓN EXITOSA!: ${outputFile.length() / 1024}KB (Archivo: ${outputFile.name})")
            return outputFile.absolutePath

        } catch (e: Exception) {
            Log.e("ImageUtils", "Error al procesar rotación/compresión", e)
            sourceFile.copyTo(outputFile, overwrite = true)
            return outputFile.absolutePath
        }
    }

    /** Funcion para eliminar la imagen guardada en el dispositivo una vez que se emita la infraccion
     * o cuando se inicie un nuevo escaneo */
    fun deleteImageFile(filePath: String?) {
        if (filePath != null) {
            try {
                val file = File(filePath)
                if (file.exists()) {
                    val deleted = file.delete()
                    println("¿Archivo borrado exitosamente?: $deleted")
                }
            } catch (e: Exception) {
                println("Error al intentar borrar la foto: ${e.message}")
            }
        }
    }
}
