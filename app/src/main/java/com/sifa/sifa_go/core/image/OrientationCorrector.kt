package com.sifa.sifa_go.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream

class OrientationCorrector : ImageProcessor {

    override fun process(input: File): File {
        val exif = ExifInterface(input.absolutePath)
        val orientation = exif.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )

        if (orientation == ExifInterface.ORIENTATION_NORMAL) return input

        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> return input
        }

        val original = BitmapFactory.decodeFile(input.absolutePath) ?: return input

        val matrix = Matrix().apply { postRotate(degrees) }
        val rotated = Bitmap.createBitmap(
            original, 0, 0, original.width, original.height, matrix, true
        )

        if (!original.isRecycled) original.recycle()

        val output = createOutputFile(input)
        FileOutputStream(output).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 100, out)
        }

        if (!rotated.isRecycled) rotated.recycle()

        return output
    }

    private fun createOutputFile(input: File): File {
        val tmpDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
        val parentDir = input.parentFile ?: File(tmpDir)
        return File(parentDir, "rotated_${input.name.orEmpty()}")
    }
}
