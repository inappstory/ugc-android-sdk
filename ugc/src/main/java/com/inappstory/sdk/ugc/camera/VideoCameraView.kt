package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.AttributeSet

class VideoCameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CameraView(context, attrs) {
    override fun createCameraService(
        cameraManager: CameraManager,
        cameraID: String
    ): CameraService {
        return VideoCameraService(cameraManager, cameraID)
    }

    override fun getTempFileExt(): String {
        return "mp4"
    }

    fun startRecording() {
        cameraIds.forEach { id->
            myCameras[id]?.let {
                if (it.isOpen()) {
                    (it as VideoCameraService).startRecording()
                    return
                }
            }
        }
    }

    fun stopRecording() {
        cameraIds.forEach { id->
            myCameras[id]?.let {
                if (it.isOpen()) {
                    (it as VideoCameraService).stopRecording()
                    return
                }
            }
        }
    }
}