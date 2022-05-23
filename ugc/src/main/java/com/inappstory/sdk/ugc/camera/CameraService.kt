package com.inappstory.sdk.ugc.camera

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import java.io.File

abstract class CameraService(
    cameraManager: CameraManager,
    cameraID: String
) {
    abstract fun startCameraPreview()
    abstract fun cameraDisconnect()
    protected var mFile: File? = null
    protected val mCameraID: String?
    protected var mCameraDevice: CameraDevice? = null
    protected var mCaptureSession: CameraCaptureSession? = null
    protected var mBackgroundHandler: Handler? = null
    protected val mCameraManager: CameraManager?
    protected var mTexture: SurfaceTexture? = null

    init {
        mCameraManager = cameraManager
        mCameraID = cameraID
    }

    fun setFile(file: File) {
        mFile = file
    }

    fun setBackgroundHandler(backgroundHandler: Handler?) {
        mBackgroundHandler = backgroundHandler
    }

    fun setTexture(texture: SurfaceTexture) {
        mTexture = texture
    }

    private val mCameraCallback: CameraDevice.StateCallback =
        object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                mCameraDevice = camera
                startCameraPreview()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDisconnect()
                mCameraDevice!!.close()
                mCameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {}
        }

    open fun isOpen(): Boolean {
        return mCameraDevice != null
    }

    abstract fun preOpenCamera()

    abstract fun stopThreadAction()

    @SuppressLint("MissingPermission")
    fun openCamera() {
        preOpenCamera()
        try {
            mCameraManager?.openCamera(mCameraID!!, mCameraCallback, mBackgroundHandler)
        } catch (ignored: CameraAccessException) {
            ignored.printStackTrace()
        }
    }

    open fun closeCamera() {
        mCameraDevice?.close()
        mCameraDevice = null
    }

}