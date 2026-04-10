package com.example.sifa_go.data.model

import com.google.gson.annotations.SerializedName

// clase padre que envuelve la respuesta del backend Python
data class DetectionRootResponse(
    val result: List<DetectionResponse>,
    val timestamp: String
)
// El backend de Python devuelve una lista de estos objetos
data class DetectionResponse(
    val plate: String?,
    val success: Boolean,
    val status: String,
    val confidence: Double?,
    // la respuesta envia este campo con el nombre image
    @SerializedName("image")
    val image_base64: String?,
    val bbox: BoundingBox?
)

data class BoundingBox(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int
)