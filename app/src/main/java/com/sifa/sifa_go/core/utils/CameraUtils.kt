package com.sifa.sifa_go.core.utils

import androidx.camera.core.ImageCapture
import androidx.camera.view.LifecycleCameraController
import java.util.concurrent.Executor

fun LifecycleCameraController.takePictureWithFlash(
    outputOptions: ImageCapture.OutputFileOptions,
    executor: Executor,
    callback: ImageCapture.OnImageSavedCallback,
    enabled: Boolean = true
) {
    setImageCaptureFlashMode(if (enabled) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF)
    takePicture(outputOptions, executor, callback)
}
