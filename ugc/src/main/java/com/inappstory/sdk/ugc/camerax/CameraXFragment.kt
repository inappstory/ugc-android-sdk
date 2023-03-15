package com.inappstory.sdk.ugc.camerax

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.*
import android.view.Surface.ROTATION_0
import android.widget.RelativeLayout
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.inappstory.sdk.stories.utils.Sizes
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.camera.CameraButton
import com.inappstory.sdk.ugc.editor.FileChooseActivity
import kotlinx.coroutines.*
import java.io.File
import java.util.concurrent.Executor
import kotlin.math.roundToInt


class CameraXFragment : Fragment(), ImageCapture.OnImageSavedCallback {
    private lateinit var imageCapture: ImageCapture
    private lateinit var preview: Preview
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var recording: Recording? = null
    private lateinit var cameraExecutor: Executor


    private lateinit var cameraButton: CameraButton
    private lateinit var changeCameraButton: View
    private lateinit var previewView: PreviewView
    private lateinit var videoProgress: CircularProgressIndicator

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.cs_camera_x_fragment, null)
    }


    private var videoIsStarted: Boolean = false
    private var limitInMillis = 30000L
    private var limitVideoInBytes = 30000000L


    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        previewView = view.findViewById<PreviewView?>(R.id.previewView).apply {
            val x = Sizes.getScreenSize().x.coerceAtMost(
                9 * Sizes.getScreenSize().y / 16
            )
            val y = Sizes.getScreenSize().y.coerceAtMost(
                16 * Sizes.getScreenSize().x / 9
            )
            layoutParams.width = x
            layoutParams.height = y
            requestLayout()
        }
        cameraButton = view.findViewById(R.id.cameraButton)
        videoProgress = view.findViewById(R.id.videoProgress)
        changeCameraButton = view.findViewById(R.id.changeCam)
        context?.let { ctx ->
            cameraExecutor = ContextCompat.getMainExecutor(ctx)
            cameraButton.actions = object : CameraButton.OnAction {
                override fun onClick() {
                    takePhoto(ctx)
                }

                override fun onLongPressDown() {
                    prepareAndStart(ctx)
                }

                override fun onLongPressUp() {
                    if (videoIsStarted) {
                        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                            stopVideo()
                        }
                    }
                }
            }
            changeCameraButton.setOnClickListener {
                flipCamera(ctx)
            }
        }
    }

    override fun onPause() {
        if (this::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }
        super.onPause()
    }

    override fun onResume() {
        startCameraPreview(requireContext())
        super.onResume()
    }

    private var currentCamera = 0

    private val cameraSelectors =
        listOf(CameraSelector.DEFAULT_BACK_CAMERA, CameraSelector.DEFAULT_FRONT_CAMERA)

    private fun flipCamera(context: Context) {
        currentCamera = (currentCamera + 1) % 2
        if (this::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }
        startCameraPreview(context)
    }

    private lateinit var cameraProvider: ProcessCameraProvider

    private fun startCameraPreview(context: Context) {
        ProcessCameraProvider.getInstance(context).let { cameraProviderFuture ->
            cameraProviderFuture.addListener(
                {
                    cameraProvider = cameraProviderFuture.get()
                    preview = Preview.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .build()
                        .also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                    imageCapture = ImageCapture.Builder()
                        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
                        .setTargetRotation(view?.display?.rotation ?: ROTATION_0)
                        .build()
                    val cameraSelector = cameraSelectors[currentCamera]
                    val recorder = Recorder.Builder()
                        .setQualitySelector(
                            QualitySelector.from(
                                Quality.HD,
                                FallbackStrategy.higherQualityOrLowerThan(Quality.SD)
                            )
                        )
                        .build()

                    videoCapture = VideoCapture.withOutput(recorder)
                    try {
                        cameraProvider.bindToLifecycle(
                            this, cameraSelector, preview, imageCapture, videoCapture
                        )
                    } catch (exc: Exception) {
                    }
                },
                cameraExecutor
            )
        }
    }

    var job: Job? = null
    private fun prepareAndStart(context: Context) {
        val fileOutput = FileOutputOptions.Builder(
            File(
                context.filesDir,
                "ugc_video.mp4"
            )
        )
            .setFileSizeLimit(limitVideoInBytes)
            .build()
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            recording = videoCapture.output
                .prepareRecording(context, fileOutput)
                .withAudioEnabled()
                .start(cameraExecutor) {
                    when (it) {
                        is VideoRecordEvent.Start -> {
                            Log.e("CameraXFragment", "Video Start")
                            videoIsStarted = true
                            job = viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                val startTime = System.currentTimeMillis()
                                while (currentTime < limitInMillis) {
                                    updateProgress()
                                    delay(100)
                                    currentTime = System.currentTimeMillis() - startTime
                                }
                                if (videoIsStarted) {
                                    stopVideo()
                                }
                            }
                        }
                        is VideoRecordEvent.Finalize -> {
                            try {
                                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                                    if (videoIsStarted) {
                                        stopVideo()
                                    }
                                    withContext(Dispatchers.Main) {
                                        openPreview(
                                            isVideo = true,
                                            filePath = it.outputResults.outputUri.path ?: ""
                                        )
                                    }
                                }
                            } catch (e: IllegalStateException) {

                            }
                        }
                    }
                }
            return
        }
    }

    var currentTime = -1L

    private suspend fun updateProgress() {
        withContext(Dispatchers.Main) {
            if (videoIsStarted) {
                val progress =
                    (100 * currentTime.toFloat() / limitInMillis.toFloat()).roundToInt()
                videoProgress.progress = progress
                videoProgress.visibility = View.VISIBLE
            }
        }
    }

    private suspend fun stopVideo() {
        job?.cancel()
        job = null
        currentTime = -1
        videoIsStarted = false
        recording?.stop()
        withContext(Dispatchers.Main) {
            cameraButton.stop()
            videoProgress.visibility = View.INVISIBLE
        }
    }

    private fun takePhoto(context: Context) {
        val outputFileOptions =
            ImageCapture.OutputFileOptions.Builder(
                File(
                    context.filesDir,
                    "ugc_photo.jpg"
                )
            ).build()
        imageCapture.takePicture(outputFileOptions, cameraExecutor, this)
    }

    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            openPreview(isVideo = false, filePath = outputFileResults.savedUri?.path ?: "")
        }
    }

    private suspend fun openPreview(isVideo: Boolean, filePath: String) {
        if (this::cameraProvider.isInitialized) {
            withContext(Dispatchers.Main) {
                cameraProvider.unbindAll()
            }
        }
        delay(300)
        withContext(Dispatchers.Main) {
            (activity as FileChooseActivity).openPreviewScreen(
                isVideo = isVideo,
                filePath = filePath
            )
        }
    }

    override fun onError(exception: ImageCaptureException) {
        Log.e("ImageCaptureError", exception.stackTraceToString())
    }
}