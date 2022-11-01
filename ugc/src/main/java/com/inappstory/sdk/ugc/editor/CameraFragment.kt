package com.inappstory.sdk.ugc.editor

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inappstory.sdk.imageloader.ImageLoader
import com.inappstory.sdk.stories.ui.video.VideoPlayer
import com.inappstory.sdk.stories.utils.Sizes
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.camera.*
import com.inappstory.sdk.ugc.camera.ImageSaveCallback
import com.inappstory.sdk.ugc.picker.FilePreviewsCache
import kotlinx.coroutines.*
import java.util.*
import kotlin.math.min

class CameraFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        isVideo = requireArguments().getBoolean("isVideo", false)
        return if (isVideo)
            inflater.inflate(R.layout.cs_video_camera_fragment, null)
        else
            inflater.inflate(R.layout.cs_camera_fragment, null)
    }

    var isVideo = false
    var cameraView: CameraView? = null
    private var activityResultLauncher: ActivityResultLauncher<Array<String>>

    init {
        this.activityResultLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { result ->
            var allAreGranted = true
            for (b in result.values) {
                allAreGranted = allAreGranted && b
            }

            if (allAreGranted) {
                openBackCamera()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cameraView?.resumeCameraView()

        if (cameraWasOpen) {
            if (lastIsBackCam)
                openBackCamera()
            else
                openFrontCamera()
        }
        cameraWasOpen = false;
    }

    private var cameraWasOpen = false;


    override fun onPause() {
        super.onPause()
        cameraView?.let {
            if (isVideo && videoStarted) {
                videoStarted = !videoStarted
                cameraButton?.stop()
                it.pauseCameraView()
                ioScope.launch {
                    delay(300)
                    stopVideo()
                }
            } else {
                it.myCameras.values.forEach { cs ->
                    if (cs.isOpen()) cameraWasOpen = true;
                }
                it.pauseCameraView()
            }

        }
    }

    val cache = FilePreviewsCache(true)

    var videoStarted = false
    var cameraButton: CameraButton? = null

    private var recordLayout: View? = null
    private var previewLayout: View? = null
    private var videoPlayer: VideoPlayer? = null

    private fun initVideoScreen(view: View) {
        recordLayout = view.findViewById(R.id.recordLayout)
        previewLayout = view.findViewById(R.id.previewLayout)
        videoPlayer = view.findViewById<VideoPlayer>(R.id.preview).also {
            it.layoutParams = RelativeLayout.LayoutParams(
                Sizes.getScreenSize(context).x,
                min(
                    (16 * Sizes.getScreenSize(context).x) / 9,
                    Sizes.getScreenSize(context).y
                )
            )
        }
        val restart = view.findViewById<FloatingActionButton>(R.id.restart)
        val changeCam = view.findViewById<FloatingActionButton>(R.id.changeCam)
        val approve = view.findViewById<FloatingActionButton>(R.id.approve)

        changeCam.setOnClickListener {
            if (!lastIsBackCam)
                openBackCamera()
            else
                openFrontCamera()
        }
        cameraButton = view.findViewById<CameraButton>(R.id.cameraButton).also {
            it.isVideoButton = true
            it.setOnClickListener {
                if (videoStarted) {
                    cameraView?.pauseCameraView()
                    ioScope.launch {
                        delay(300)
                        stopVideo()
                        videoStarted = !videoStarted
                    }
                } else {
                    ioScope.launch {
                        delay(300)
                        startVideo()

                        videoStarted = !videoStarted
                    }
                }
            }
        }
        restart.setOnClickListener {
            previewLayout?.visibility = View.GONE
            videoPlayer?.destroy()
            recordLayout?.visibility = View.VISIBLE
            cameraView?.resumeCameraView()
            if (lastIsBackCam)
                openBackCamera()
            else
                openFrontCamera()
        }
        approve.setOnClickListener {
            sendResult()
        }
    }

    var lastIsBackCam = false

    private fun showPreview() {
        mainScope.launch {
            recordLayout?.visibility = View.GONE
            previewLayout?.visibility = View.VISIBLE
        }
    }

    private val mainScope = CoroutineScope(Dispatchers.Main)
    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private fun initPhotoScreen(view: View) {
        var videoStarted = false
        recordLayout = view.findViewById(R.id.recordLayout)
        previewLayout = view.findViewById(R.id.previewLayout)
        val restart = view.findViewById<FloatingActionButton>(R.id.restart)
        val changeCam = view.findViewById<FloatingActionButton>(R.id.changeCam)
        val approve = view.findViewById<FloatingActionButton>(R.id.approve)
        val preview = view.findViewById<ImageView>(R.id.preview).also {
            it.layoutParams = RelativeLayout.LayoutParams(
                Sizes.getScreenSize(context).x,
                min(
                    (16 * Sizes.getScreenSize(context).x) / 9,
                    Sizes.getScreenSize(context).y
                )
            )
        }
        changeCam.setOnClickListener {
            if (!lastIsBackCam)
                openBackCamera()
            else
                openFrontCamera()
        }
        cameraButton = view.findViewById<CameraButton>(R.id.cameraButton).also {
            it.setOnClickListener {
                ioScope.launch {
                    makePhoto(saveAction = {
                        mainScope.launch(Dispatchers.Main) {
                            cache.loadPreview(it, preview, false)
                        }
                    }, action = {
                        showPreview()
                    })
                }
            }
        }
        restart.setOnClickListener {
            previewLayout?.visibility = View.GONE
            recordLayout?.visibility = View.VISIBLE
            preview.setImageBitmap(null)
            cameraView?.resumeCameraView()
            if (lastIsBackCam)
                openBackCamera()
            else
                openFrontCamera()
        }
        approve.setOnClickListener {
            sendResult()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraView = view.findViewById(R.id.cameraView)
        cameraView?.setFileName(
            requireArguments()
                .getString("fileName", UUID.randomUUID().toString())
        );
        val appPerms = if (isVideo) {
            arrayOf(
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA
            )
        } else {
            arrayOf(
                Manifest.permission.CAMERA
            )
        }
        if (isVideo) initVideoScreen(view)
        else initPhotoScreen(view)
        activityResultLauncher.launch(appPerms)
    }

    private fun openBackCamera() {
        lastIsBackCam = true
        cameraView?.openBackCamera()
    }

    private fun openFrontCamera() {

        lastIsBackCam = false
        cameraView?.openFrontCamera()
    }

    private fun makePhoto(action: () -> Unit, saveAction: (path: String) -> Unit) {
        (cameraView!! as PhotoCameraView).makePhoto(
            object : ImageSaveCallback {
                override fun onSave(path: String) {
                    saveAction(path)
                }

            })
        action()
    }

    private fun startVideo() {
        delayedStopJob = startDelayedJob(delayTime = 30000) {
            delayedStopJob = null
            if (videoStarted) {
                mainScope.launch {
                    cameraButton?.stop()
                }
                ioScope.launch {
                    cameraView?.pauseCameraView()
                    delay(300)
                    stopVideo()
                    videoStarted = !videoStarted
                }
            }
        }
        (cameraView!! as VideoCameraView).apply {
            setForceStopCallback(object : VideoForceStopCallback {
                override fun onStop() {
                    if (isVideo && videoStarted) {
                        clearDelayedJob()
                        videoStarted = !videoStarted
                        cameraButton?.stop()
                        cameraView?.pauseCameraView()
                        ioScope.launch {
                            delay(300)
                            (cameraView!! as VideoCameraView).stopRecording()
                            showPreview()
                            videoPlayer?.loadVideo(cameraView!!.filePath!!)
                        }
                    }
                }
            })
            startRecording()
        }
    }

    private var delayedStopJob: Job? = null

    private fun clearDelayedJob() {
        delayedStopJob?.cancel()
        delayedStopJob = null
    }

    private fun stopVideo() {
        clearDelayedJob()
        (cameraView!! as VideoCameraView).stopRecording()
        showPreview()
        ioScope.launch {
            delay(300)
            videoPlayer?.loadVideo(cameraView!!.filePath!!)
        }
    }

    private fun sendResult() {
        if (cameraView != null) {
            (activity as FileChooseActivity).sendResult(cameraView!!.filePath!!)
        }
    }
}

fun startDelayedJob(delayTime: Long = 500, action: suspend () -> Unit) =
    CoroutineScope(Dispatchers.Default).launch {
        delay(delayTime)
        action.invoke()
    }
