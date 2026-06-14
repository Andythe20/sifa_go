package com.sifa.sifa_go.core.image

import java.io.File

fun interface ImageProcessor {
    fun process(input: File): File
}
