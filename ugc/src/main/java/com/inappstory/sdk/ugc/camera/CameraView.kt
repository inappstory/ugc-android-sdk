package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import java.io.File

abstract class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    val myCameras: HashMap<Int, CameraService> = hashMapOf()

    private val cam0: Int = 0
    private val cam1: Int = 1

    val cameraIds = listOf(cam0, cam1)

    protected var mImageView: TextureView? = null
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mCameraManager: CameraManager? = null


    private fun stopBackgroundThread() {
        mBackgroundThread?.quitSafely()
        try {
            cameraIds.forEach { id ->
                myCameras[id]?.stopThreadAction()
                myCameras[id]?.setBackgroundHandler(null)
            }
            mBackgroundThread?.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }


    private fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("CameraBackground")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
        cameraIds.forEach { id -> myCameras[id]?.setBackgroundHandler(mBackgroundHandler) }
    }


    init {
        mImageView = TextureView(context).also {
            it.layoutParams = LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            addView(it)
        }
        mCameraManager = (context.getSystemService(Context.CAMERA_SERVICE) as CameraManager).also {
            try {
                for (cameraID in it.cameraIdList) {
                    val id = cameraID.toInt()
                    myCameras[id] = VideoCameraService(it, cameraID).apply {
                        setFile(File("${context.cacheDir}/${getTempFileName()}"))
                    }
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

    }

    open fun pauseCameraView() {
        cameraIds.forEach { id ->
            myCameras[id]?.let {
                if (it.isOpen())
                    it.closeCamera()
            }
        }
        stopBackgroundThread()
    }

    open fun resumeCameraView() {
        startBackgroundThread()
        cameraIds.forEach { id -> myCameras[id]?.setTexture(mImageView!!.surfaceTexture!!) }
    }

    open fun openCamera1() {
        myCameras[cam0]?.let {
            if (it.isOpen()) it.closeCamera()
        }
        myCameras[cam1]?.let {
            it.setTexture(mImageView!!.surfaceTexture!!)
            if (!it.isOpen()) it.openCamera()
        }
    }

    open fun openCamera2() {
        myCameras[cam1]?.let {
            if (it.isOpen()) it.closeCamera()
        }
        myCameras[cam0]?.let {
            it.setTexture(mImageView!!.surfaceTexture!!)
            if (!it.isOpen()) it.openCamera()
        }
    }

    abstract fun getTempFileName(): String
}