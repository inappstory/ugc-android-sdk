package com.inappstory.sdk.ugc.camera

import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import android.view.Surface
import kotlinx.coroutines.*

import java.lang.Exception
import kotlin.math.min

class VideoCameraService(cameraManager: CameraManager, cameraID: String) :
    CameraService(cameraManager, cameraID) {

    private var mMediaRecorder: MediaRecorder? = null
    private var mPreviewBuilder: CaptureRequest.Builder? = null
    private var recorderSurface: Surface? = null

    var recorderStarted = false

    private fun stopMediaRecorder() {
        if (recorderStarted)
            mMediaRecorder?.stop()
        recorderStarted = false
        mMediaRecorder?.release()
        mMediaRecorder = null
    }


    override fun startCameraPreview() {
        mTexture!!.setDefaultBufferSize(1280, 720)
        var surface = Surface(mTexture)

        try {
            mPreviewBuilder =
                mCameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)?.also {
                    it.addTarget(surface)
                }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                recorderSurface = MediaCodec.createPersistentInputSurface()
                mMediaRecorder?.setInputSurface(recorderSurface!!)
            }
            try {
                mMediaRecorder?.prepare()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            mPreviewBuilder?.addTarget(recorderSurface!!)
            mCameraDevice?.createCaptureSession(
                listOf(surface, recorderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCaptureSession = session
                        try {
                            mCaptureSession!!.setRepeatingRequest(
                                mPreviewBuilder!!.build(),
                                null,
                                mBackgroundHandler
                            )
                        } catch (e: CameraAccessException) {
                            e.printStackTrace()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }
                }, mBackgroundHandler
            )
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    override fun cameraDisconnect() {
        stopMediaRecorder()
    }

    var stopCallback: VideoForceStopCallback? = null

    private fun setUpMediaRecorder() {
        val profile: CamcorderProfile = when {
            CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P) -> {
                CamcorderProfile.get(CamcorderProfile.QUALITY_1080P)
            }
            CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P) -> {
                CamcorderProfile.get(CamcorderProfile.QUALITY_720P)
            }
            CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_HIGH) -> {
                CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH)
            }
            else -> {
                return
            }
        }
        stopMediaRecorder()
        mMediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOrientationHint(90)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(mFile!!.absolutePath)
            setVideoFrameRate(profile.videoFrameRate)
            setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight)
            setVideoEncodingBitRate(min(profile.videoBitRate, 3500000))
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(min(profile.audioBitRate, 256000))
            setAudioSamplingRate(profile.audioSampleRate)
            setMaxDuration(30000) //Limit video size to 30 sec
            setMaxFileSize(30000000) //Limit video size to 30 mb

        }.also {
            it.setOnInfoListener { _, what, _ ->
                when (what) {
                    MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED,
                    MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED,
                    MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> {
                        stopCallback?.onStop()
                    }
                    else -> {
                        Log.e("onInfoListener", "$what")
                    }
                }
            }
        }
    }

    private val recordCoroutineScope =
        CoroutineScope(Dispatchers.IO)


    fun startRecording() {
        recordCoroutineScope.launch {
            mMediaRecorder!!.start()
            recorderStarted = true
        }

    }

    fun stopRecording() {
        recordCoroutineScope.launch {
            stopMediaRecorder()
        }
    }

    override fun preOpenCamera() {
        setUpMediaRecorder()
    }

    override fun stopThreadAction() {
        stopMediaRecorder()
    }
}

