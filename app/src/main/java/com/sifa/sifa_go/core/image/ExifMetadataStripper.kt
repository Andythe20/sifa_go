package com.sifa.sifa_go.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

class ExifMetadataStripper : ImageProcessor {

    override fun process(input: File): File {
        if (!hasExifMetadata(input)) return input

        val bitmap = BitmapFactory.decodeFile(input.absolutePath)
            ?: return input

        val output = createOutputFile(input)
        FileOutputStream(output).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        if (!bitmap.isRecycled) bitmap.recycle()

        return output
    }

    private fun hasExifMetadata(file: File): Boolean {
        return try {
            val exif = ExifInterface(file.absolutePath)
            exif.latLong != null ||
                exif.getAttribute(ExifInterface.TAG_MAKE) != null ||
                exif.getAttribute(ExifInterface.TAG_MODEL) != null ||
                exif.getAttribute(ExifInterface.TAG_DATETIME) != null
        } catch (_: Exception) {
            true
        }
    }

    private fun createOutputFile(input: File): File {
        val parentDir = input.parentFile
            ?: File(System.getProperty("java.io.tmpdir") ?: "/tmp")
        return File(parentDir, "stripped_${input.name.orEmpty()}")
    }
}
