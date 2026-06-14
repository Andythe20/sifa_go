package com.sifa.sifa_go.core.image

import java.io.File

object ImageSanitizer {

    private val pipeline = ImageProcessingPipeline(
        OrientationCorrector(),
        ExifMetadataStripper(),
        ImageCompressor()
    )

    private val lightPipeline = ImageProcessingPipeline(
        OrientationCorrector(),
        ExifMetadataStripper()
    )

    fun sanitize(file: File): File = pipeline.process(file)

    fun sanitizeLight(file: File): File = lightPipeline.process(file)
}
