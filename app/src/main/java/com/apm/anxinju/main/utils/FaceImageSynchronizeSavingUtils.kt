package com.apm.anxinju.main.utils

import android.graphics.Bitmap
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.coroutines.CoroutineContext
import kotlin.system.measureTimeMillis

/**
 *  author : ciih
 *  date : 2019-09-28 17:16
 *  description :
 */

object FaceImageSynchronizeSavingUtils : CoroutineScope {

    private val coroutineDispatcher =
        Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    override val coroutineContext: CoroutineContext
        get() = coroutineDispatcher

    private val faceFile = File(
        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
        "image_captured.jpg"
    )

    fun saveNewImage(image: Bitmap): File {
        var fileOutputStream: FileOutputStream? = null
        try {
            fileOutputStream = FileOutputStream(faceFile)
            image.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream)
            fileOutputStream.flush()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
        return faceFile
    }


    fun getImageCopy(process: suspend (File) -> Unit) = measureTimeMillis {
        val tempFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
            "image_captured_${System.currentTimeMillis()}.jpg"
        )
        try {
            faceFile.copyTo(
                tempFile,
                overwrite = true
            )
        } finally {

        }
        launch {
            process(faceFile)
            tempFile.delete()
        }
    }.apply {
        println("---------------->get image: $this")
    }
}