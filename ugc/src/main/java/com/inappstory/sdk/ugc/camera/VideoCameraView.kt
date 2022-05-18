package com.inappstory.sdk.ugc.camera

import android.content.Context
import android.util.AttributeSet

class VideoCameraView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : CameraView(context, attrs) {
    override fun getTempFileName(): String {
        return "test.mp4"
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