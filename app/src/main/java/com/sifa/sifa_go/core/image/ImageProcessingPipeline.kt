package com.sifa.sifa_go.core.image

import java.io.File

class ImageProcessingPipeline(
    private vararg val steps: ImageProcessor
) : ImageProcessor {

    override fun process(input: File): File {
        var current = input
        for (step in steps) {
            current = step.process(current)
            cleanIntermediate(current, input)
        }
        return current
    }

    private fun cleanIntermediate(processed: File, original: File) {
        if (processed.absolutePath != original.absolutePath) {
            processed.deleteOnExit()
        }
    }
}
