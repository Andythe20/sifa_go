package com.sifa.sifa_go.core.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream

class ImageCompressor(
    private val quality: Int = 80,
    private val format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG
) : ImageProcessor {

    override fun process(input: File): File {
        val bitmap = BitmapFactory.decodeFile(input.absolutePath) ?: return input

        val output = File(input.parentFile, "compressed_${input.name}")
        FileOutputStream(output).use { out ->
            bitmap.compress(format, quality, out)
        }

        if (!bitmap.isRecycled) bitmap.recycle()

        return output
    }
}
