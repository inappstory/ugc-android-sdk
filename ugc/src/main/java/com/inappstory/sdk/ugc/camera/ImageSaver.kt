package com.inappstory.sdk.ugc.camera

import android.media.Image
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
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