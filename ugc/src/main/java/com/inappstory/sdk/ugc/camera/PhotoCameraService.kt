package com.inappstory.sdk.ugc.camera

import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.hardware.camera2.CameraCaptureSession.CaptureCallback
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.view.Surface

class PhotoCameraService(cameraManager: CameraManager, cameraID: String) :
    CameraService(cameraManager, cameraID) {

    private lateinit var mImageReader: ImageReader

    private val mOnImageAvailableListener =
        OnImageAvailableListener { reader ->
            mBackgroundHandler?.post(
                ImageSaver(reader.acquireNextImage(), mFile!!)
            )
        }

    override fun startCameraPreview() {
        mImageReader = ImageReader.newInstance(1920, 1080, ImageFormat.JPEG, 1)
        mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, null)

        mTexture!!.setDefaultBufferSize(1920, 1080)
        val surface = Surface(mTexture)

        try {
            val builder = mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            builder.addTarget(surface)
            if (mBackgroundHandler != null) mCameraDevice!!.createCaptureSession(
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

    fun makePhoto() {
        try {
            val captureBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(mImageReader.surface)
            val captureCallback: CaptureCallback = object : CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                }
            }
            mCaptureSession!!.stopRepeating()
            mCaptureSession!!.abortCaptures()
            mCaptureSession!!.capture(captureBuilder.build(), captureCallback, mBackgroundHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun preOpenCamera() {

    }

    override fun stopThreadAction() {

    }
}