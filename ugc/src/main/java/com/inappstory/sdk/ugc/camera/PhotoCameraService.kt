package com.inappstory.sdk.ugc.camera

import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.hardware.camera2.CameraCharacteristics


internal class PhotoCameraService(cameraManager: CameraManager, cameraID: String) :
    CameraService(cameraManager, cameraID) {

    private lateinit var mImageReader: ImageReader

    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader ->
            mBackgroundHandler?.post(
                ImageSaver(reader.acquireNextImage(), mFile!!, saveCallback)
            )
        }


    private var saveCallback: ImageSaveCallback? = null

    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    override fun startCameraPreview() {
        val size = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP
        )!!
            .getOutputSizes(ImageFormat.JPEG).maxByOrNull { it.height * it.width }!!
        mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 3)
        mTexture!!.setDefaultBufferSize(1920, 1080)
        val surface = Surface(mTexture)

        try {
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            if (mBackgroundHandler != null)

                mCameraDevice!!.createCaptureSession(
                    listOf(surface, mImageReader.surface),
                    object : CameraCaptureSession.StateCallback() {
                        override fun onConfigured(session: CameraCaptureSession) {
                            mCaptureSession = session
                            try {
                                mBackgroundHandler?.let {
                                    mCaptureSession?.setRepeatingRequest(
                                        builder.build(),
                                        null,
                                        it
                                    )
                                }
                            } catch (e: CameraAccessException) {
                                e.printStackTrace()
                            }
                        }

                        override fun onConfigureFailed(session: CameraCaptureSession) {}
                    }, mBackgroundHandler
                )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun cameraDisconnect() {

    }

    val characteristics: CameraCharacteristics =
        cameraManager.getCameraCharacteristics(cameraID)

    fun makePhoto(saveCallback: ImageSaveCallback) {
        try {
            this.saveCallback = saveCallback
            while (mImageReader.acquireNextImage() != null) {
            }
            mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, imageReaderHandler)
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    .apply { addTarget(mImageReader.surface) }
            captureBuilder.set(
                CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(characteristics)
            )
            var builder = captureBuilder.build()
            mCaptureSession!!.capture(
                builder,
                object : CaptureCallback() {}, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    val ORIENTATIONS = mapOf(
        Surface.ROTATION_0 to 90,
        Surface.ROTATION_90 to 0,
        Surface.ROTATION_180 to 270,
        Surface.ROTATION_270 to 180
    )


    private fun getJpegOrientation(c: CameraCharacteristics, lDeviceOrientation: Int = 0): Int {
        var deviceOrientation = ORIENTATIONS[lDeviceOrientation]!!
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        val facingFront =
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        return (sensorOrientation + deviceOrientation + 270) % 360
    }

    override fun preOpenCamera() {

    }

    override fun stopThreadAction() {

    }
}