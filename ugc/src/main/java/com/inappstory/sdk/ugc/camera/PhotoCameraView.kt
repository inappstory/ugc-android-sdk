package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.util.AttributeSet

class PhotoCameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CameraView(context, attrs) {

    fun makePhoto() {
        cameraIds.forEach { id->
            myCameras[id]?.let {
                if (it.isOpen()) {
                    it.setTexture(mImageView!!.surfaceTexture!!)
                    (it as PhotoCameraService).makePhoto()
                    it.closeCamera()
                    return
                }
            }
        }
    }

    override fun getTempFileName(): String {
        return "test.jpg"
    }
}