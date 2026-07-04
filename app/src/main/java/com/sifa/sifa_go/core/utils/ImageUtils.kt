package com.sifa.sifa_go.core.utils

import android.content.Context
import android.util.Log
import com.sifa.sifa_go.core.image.ImageSanitizer
import java.io.File

object ImageUtils {

    @Deprecated(
        message = "Usar ImageSanitizer.sanitize() en su lugar. compressImage será eliminado en futuras versiones.",
        replaceWith = ReplaceWith("ImageSanitizer.sanitize(sourceFile).absolutePath")
    )
    fun compressImage(context: Context, sourceFile: File): String {
        val outputFolder = File(context.filesDir, "evidencia_multas")
        if (!outputFolder.exists()) outputFolder.mkdirs()

        val outputFile = File(outputFolder, "patente_${System.currentTimeMillis()}.jpg")

        try {
            val sanitized = ImageSanitizer.sanitize(sourceFile)
            sanitized.copyTo(outputFile, overwrite = true)
            Log.e("ImageUtils", "¡SANITIZACIÓN EXITOSA!: ${outputFile.length() / 1024}KB (Archivo: ${outputFile.name})")
            return outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error al sanitizar imagen", e)
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
