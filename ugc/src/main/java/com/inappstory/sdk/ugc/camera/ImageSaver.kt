package com.inappstory.sdk.ugc.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.media.Image
import com.inappstory.sdk.InAppStoryService
import com.inappstory.sdk.stories.utils.Sizes
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer

internal class ImageSaver(
    private val image: Image,
    private val file: File,
    private var callback: ImageSaveCallback? = null
) : Runnable {



    override fun run() {
        val buffer: ByteBuffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer[bytes]
        var output: FileOutputStream? = null
        try {
            output = FileOutputStream(file)
            output.write(bytes)
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            image.close()
            if (null != output) {
                try {
                    output.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            callback?.onSave(file.absolutePath)
        }
    }
}