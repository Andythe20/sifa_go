package com.sifa.sifa_go.core.image

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

fun File.toCleanMultipartPart(
    partName: String,
    sanitize: (File) -> File = ImageSanitizer::sanitize
): MultipartBody.Part {
    val cleanFile = sanitize(this)
    val requestFile = cleanFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
    return MultipartBody.Part.createFormData(partName, cleanFile.name, requestFile)
}

fun List<File>.toCleanMultipartParts(
    partName: String = "fotos",
    sanitize: (File) -> File = ImageSanitizer::sanitize
): List<MultipartBody.Part> = map { it.toCleanMultipartPart(partName, sanitize) }
