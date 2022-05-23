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
import android.view.OrientationEventListener


internal class PhotoCameraService(cameraManager: CameraManager, cameraID: String) :
    CameraService(cameraManager, cameraID) {

    private lateinit var mImageReader: ImageReader

    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader ->
            mBackgroundHandler?.post(
                ImageSaver(reader.acquireNextImage(), mFile!!,saveCallback)
            )
        }


    private var saveCallback: ImageSaveCallback? = null

    private val imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    private val imageReaderHandler = Handler(imageReaderThread.looper)

    override fun startCameraPreview() {
        val size = characteristics.get(
            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!
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
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION,
                getJpegOrientation(characteristics))
            var builder = captureBuilder.build()
            mCaptureSession!!.capture(
                builder,
                object : CaptureCallback() {
                    override fun onCaptureStarted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        timestamp: Long,
                        frameNumber: Long
                    ) {
                        super.onCaptureStarted(session, request, timestamp, frameNumber)
                    }

                    override fun onCaptureCompleted(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        result: TotalCaptureResult
                    ) {
                        super.onCaptureCompleted(session, request, result)
                    }

                    override fun onCaptureBufferLost(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        target: Surface,
                        frameNumber: Long
                    ) {
                        super.onCaptureBufferLost(session, request, target, frameNumber)
                    }

                    override fun onCaptureFailed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        failure: CaptureFailure
                    ) {
                        super.onCaptureFailed(session, request, failure)
                    }

                    override fun onCaptureProgressed(
                        session: CameraCaptureSession,
                        request: CaptureRequest,
                        partialResult: CaptureResult
                    ) {
                        super.onCaptureProgressed(session, request, partialResult)
                    }

                    override fun onCaptureSequenceAborted(
                        session: CameraCaptureSession,
                        sequenceId: Int
                    ) {
                        super.onCaptureSequenceAborted(session, sequenceId)
                    }

                    override fun onCaptureSequenceCompleted(
                        session: CameraCaptureSession,
                        sequenceId: Int,
                        frameNumber: Long
                    ) {
                        super.onCaptureSequenceCompleted(session, sequenceId, frameNumber)
                    }
                }, mBackgroundHandler
            )
            //  mCaptureSession!!.stopRepeating()
            //  mCaptureSession!!.abortCaptures()
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    private fun getJpegOrientation(c: CameraCharacteristics, lDeviceOrientation: Int = 0): Int {
        var deviceOrientation = lDeviceOrientation
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return 0
        val sensorOrientation = c.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        // Round device orientation to a multiple of 90
        deviceOrientation = (deviceOrientation + 45) / 90 * 90

        // Reverse device orientation for front-facing cameras
        val facingFront =
            c.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
        if (facingFront) deviceOrientation = -deviceOrientation

        // Calculate desired JPEG orientation relative to camera orientation to make
        // the image upright relative to the device orientation
        return (sensorOrientation + deviceOrientation + 360) % 360
    }

    override fun preOpenCamera() {

    }

    override fun stopThreadAction() {

    }
}