package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.AttributeSet

class PhotoCameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CameraView(context, attrs) {

    override fun createCameraService(
        cameraManager: CameraManager,
        cameraID: String
    ): CameraService {
        return PhotoCameraService(cameraManager, cameraID)
    }

    internal fun makePhoto(saveCallback: ImageSaveCallback) {
        cameraIds.forEach { id->
            myCameras[id]?.let {
                if (it.isOpen()) {
                    it.setTexture(mImageView!!.surfaceTexture!!)
                    (it as PhotoCameraService).makePhoto(saveCallback)
                    return
                }
            }
        }
    }

    override fun getTempFileExt(): String {
        return "jpg"
    }
}