package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.TextureView
import android.view.ViewGroup
import android.widget.FrameLayout
import com.inappstory.sdk.stories.utils.Sizes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import kotlin.math.max
import kotlin.math.min

abstract class CameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {
    val myCameras: HashMap<Int, CameraService> = hashMapOf()

    private val back: Int = 0
    private val front: Int = 1

    val cameraIds = listOf(back, front)

    protected var mImageView: TextureView? = null
    private var mBackgroundThread: HandlerThread? = null
    private var mBackgroundHandler: Handler? = null
    private var mCameraManager: CameraManager? = null

    private var fileName: String? = "test"
    var filePath: String? = null

    fun setFileName(fileName: String) {
        this.fileName = fileName
        filePath = "${context.cacheDir}/$fileName.${getTempFileExt()}"
        myCameras.values.forEach {
            it.setFile(File(filePath))
        }
    }

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
        GlobalScope.launch(Dispatchers.Main) {
            mBackgroundThread = HandlerThread("CameraBackground")
                .apply {
                    start()
                    mBackgroundHandler = Handler(this.looper)
                }
            cameraIds.forEach { id -> myCameras[id]?.setBackgroundHandler(mBackgroundHandler) }
        }
    }


    init {
        mImageView = TextureView(context).also {
            it.layoutParams = LayoutParams(
                Sizes.getScreenSize(context).x,
                min(
                    (16 * Sizes.getScreenSize(context).x) / 9,
                    Sizes.getScreenSize(context).y
                )
            )
            addView(it)
        }
        mImageView?.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                surfaceTextureReady()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return false
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

            }
        }
        if (fileName != null) {
            filePath = "${context.cacheDir}/$fileName.${getTempFileExt()}"
        }
        mCameraManager = (context.getSystemService(Context.CAMERA_SERVICE)
                as CameraManager).also {
            try {
                for (cameraID in it.cameraIdList) {
                    val id = cameraID.toInt()
                    myCameras[id] = createCameraService(it, cameraID).apply {
                        if (filePath != null) {
                            setFile(File(filePath))
                        }
                    }
                }
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

    }

    abstract fun createCameraService(cameraManager: CameraManager, cameraID: String): CameraService

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

        GlobalScope.launch(Dispatchers.Main) {
            mImageView!!.surfaceTexture?.let {
                if (myCameras == null || myCameras.size == 0) return@launch
                if (myCameras[back] != null) {
                    myCameras[back]!!.setTexture(it)
                }
                if (myCameras.size == 1) return@launch
                if (myCameras[front] != null) {
                    myCameras[front]!!.setTexture(it)
                }
            }
        }
    }

    fun surfaceTextureReady() {
        GlobalScope.launch(Dispatchers.Main) {
            mImageView!!.surfaceTexture?.let {
                cameraIds.forEach { id ->
                    myCameras[id]?.setTexture(it)
                }
            }
        }
    }

    open fun openBackCamera() {

        myCameras[front]?.let {
            if (it.isOpen()) it.closeCamera()
        }
        myCameras[back]?.let {
            mImageView!!.surfaceTexture?.let { st ->
                it.setTexture(st)
                if (!it.isOpen()) it.openCamera()
            }
        }
    }

    open fun openFrontCamera() {
        myCameras[back]?.let {
            if (it.isOpen()) it.closeCamera()
        }
        myCameras[front]?.let {
            mImageView!!.surfaceTexture?.let { st ->
                it.setTexture(st)
                if (!it.isOpen()) it.openCamera()
            }
        }
    }

    abstract fun getTempFileExt(): String
}