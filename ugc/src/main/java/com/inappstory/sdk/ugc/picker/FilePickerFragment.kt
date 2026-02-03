package com.inappstory.sdk.ugc.picker

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.MimeTypeMap
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.camerax.BackPressedFragment
import com.inappstory.sdk.ugc.utils.faststart.FastStart
import java.io.File
import java.io.FileOutputStream
import java.lang.Exception
import java.util.UUID


internal class FilePickerFragment : BackPressedFragment() {


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {


        return inflater.inflate(R.layout.cs_file_picker_fragment, null)
    }

    private lateinit var uploadButton: FloatingActionButton
    private lateinit var previews: FilePreviewsList

    var acceptTypes = arrayListOf<String>()
    var allowMultiple = true
    val selectedFiles = arrayListOf<SelectedFile>()


    private val STORAGE_PERMISSIONS_RESULT = 888
    private val CAMERA_PERMISSIONS_RESULT = 890

    private fun checkStoragePermissions() {
        activity?.apply {
            if (!loaded || previews.adapter?.itemCount == 0)
                loadPreviews(true)
        }
    }

    private fun checkCameraPermissions() {
        activity?.apply {
            var allGranted = true;
            val localPerms =
                arrayListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
            localPerms.forEach {
                if (ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    allGranted = false;
                }
            }
            if (!allGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(
                        localPerms.toTypedArray(),
                        CAMERA_PERMISSIONS_RESULT
                    )
                }
            } else {
                openCameraScreen()
            }
        }
    }

    private var loaded = false
    private var dialogShown = false

    private val appPerms = emptyArray<String>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uploadButton = view.findViewById(R.id.upload)
        previews = view.findViewById(R.id.previews)
        arguments?.apply {
            val messageNames = getStringArray("messageNames")
            val messageValues = getStringArray("messages")
            if (messageNames != null && messageValues != null) {
                messages.putAll(messageNames.zip(messageValues).toMap())
            }
            acceptTypes = getStringArrayList("acceptTypes") ?: arrayListOf()
            allowMultiple = getBoolean("allowMultiple", false)
            messages["button_no_gallery_access"]?.let {
                galleryAccessText = it
            }
        }
        if (acceptTypes.isEmpty()) {
            activity?.onBackPressed()
            return
        }
        uploadButton.setOnClickListener {
            if (activity is FileChooseActivity && selectedFiles.isNotEmpty()) {
                (activity as FileChooseActivity).sendResultMultiple(convertFiles().toTypedArray())
            }
        }
    }

    private lateinit var pickSingleVisualMediaCallback: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var pickMultipleVisualMediaCallback: ActivityResultLauncher<PickVisualMediaRequest>

    private fun openGooglePhotoPicker() {
        var allowVideo = false
        var allowPhoto = false
        acceptTypes.forEach {
            if (it.contains("video")) allowVideo = true
            if (it.contains("image")) allowPhoto = true
        }
        val allowAll = allowPhoto && allowVideo


        val visualMediaType: ActivityResultContracts.PickVisualMedia.VisualMediaType =
            if (allowAll) {
                ActivityResultContracts.PickVisualMedia.ImageAndVideo
            } else if (allowPhoto) {
                ActivityResultContracts.PickVisualMedia.ImageOnly
            } else if (allowVideo) {
                ActivityResultContracts.PickVisualMedia.VideoOnly
            } else {
                return
            }
        if (allowMultiple) {
            pickMultipleVisualMediaCallback.launch(PickVisualMediaRequest(visualMediaType))
        } else {
            pickSingleVisualMediaCallback.launch(PickVisualMediaRequest(visualMediaType))
        }
    }

    private fun convertFiles(): ArrayList<String> {
        val resultFiles = arrayListOf<String>()
        selectedFiles.forEach {
            if (it.fileType == "video") {
                val currentFile = File(it.filePath)
                currentFile.extension
                val file = File(
                    "${requireContext().filesDir}/converted",
                    "${UUID.randomUUID()}_${it.filePath}"
                )
                val fs = FastStart(currentFile.absolutePath, file.absolutePath).fastStart()
                if (fs) {
                    resultFiles.add(file.absolutePath)
                } else {
                    resultFiles.add(currentFile.absolutePath)
                }
            } else {
                resultFiles.add(it.filePath)
            }
        }
        return resultFiles
    }

    override fun onStart() {
        super.onStart()
        checkStoragePermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pickSingleVisualMediaCallback =
            registerForActivityResult(
                ActivityResultContracts.PickVisualMedia()
            ) { uri ->
                // Callback is invoked after the user selects media items or closes the
                // photo picker.
                uri?.let {
                    it.toFile().absolutePath
                    it.path?.let { path ->

                    }
                }
            }
        pickMultipleVisualMediaCallback =
            registerForActivityResult(
                ActivityResultContracts.PickMultipleVisualMedia(10)
            ) { uris ->
                // Callback is invoked after the user selects media items or closes the
                // photo picker.
                if (uris.isNotEmpty()) {
                    uris.forEach {
                        val mimeType = it.getMimeType(requireContext())
                        val file = writeToTempFile(requireContext(), it, mimeType)
                        selectedFiles.add(
                            SelectedFile(
                                file.absolutePath,
                                mimeType
                            )
                        )
                        // Log.d("PhotoPicker", uriAccessed.toString())

                    }
                    if (activity is FileChooseActivity && selectedFiles.isNotEmpty()) {
                        (activity as FileChooseActivity).sendResultMultiple(convertFiles().toTypedArray())
                    }
                } else {
                    Log.d("PhotoPicker", "No media selected")

                }
            }
    }

    private fun Uri.grantAppAccess(context: Context): Uri? {
        return try {
            val fileExtension = context.contentResolver.getType(this)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?: "tmp" // Default to "tmp" if the extension cannot be determined

            val inputStream = context.contentResolver.openInputStream(this)
                ?: throw Exception("Input stream is null.")

            val tempFile =
                File(context.cacheDir, "upload_${System.currentTimeMillis()}.$fileExtension")

            // Write the input stream's data to the temporary file
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            // Close the input stream after usage
            inputStream.close()

            // Generate a URI for the temp file using FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.inappstory.sdk.ugc.fileProvider",
                tempFile
            )

        } catch (e: Exception) {
            null
        }
    }

    private fun Uri.getMimeType(context: Context): String = context.contentResolver.getType(this)
        ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
        ?: "tmp"

    private fun writeToTempFile(context: Context, uri: Uri, mimeType: String): File {
        val dir = File(context.cacheDir.absolutePath + File.separator + "ias_ugc")
        dir.mkdirs()
        val tempFile = File.createTempFile("ias_ugc_file", ".$mimeType", dir)
        context.contentResolver.openInputStream(uri).use { inputStream ->
            FileOutputStream(tempFile).use { outputStream ->
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                while (inputStream?.read(buffer).also { bytesRead = it ?: -1 } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }
            }
        }
        return tempFile
    }


    private var galleryAccessText = "Tap to allow access to your Gallery"
    private val messages = hashMapOf<String, String>()
    private val storageDefault =
        "You need storage access to load photos and videos. Tap Settings > Permissions and turn \'Files and media\' on"
    private val videoDefault =
        "You need camera and microphone access to make photos and videos. Tap Settings > Permissions and turn 'Camera' and 'Microphone' on"


    fun requestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        var allGranted = true;
        val positiveText = messages["dialog_button_settings"]
        val negativeText = messages["dialog_button_not_now"]
        if (requestCode == CAMERA_PERMISSIONS_RESULT) {
            if (grantResults.isNotEmpty()) {
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != 0)
                        allGranted = false;
                }
                if (!allGranted)
                    openSettingsDialog(
                        text = messages.getOrElse(
                            "dialog_video_permissions_warning",
                            defaultValue = { videoDefault }),
                        positiveText = positiveText,
                        negativeText = negativeText,
                    )
                else
                    openCameraScreen()
            } else {
                return;
            }

        }
    }

    private fun loadPreviews(hasFileAccess: Boolean) {
        loaded = hasFileAccess

        val galleryFileLimitText =
            messages["warns_file_picker_files_limit"] ?: "You can select up to 10 files"
        val allowMultiple = arguments?.getBoolean("allowMultiple") ?: false
        val filePickerFilesLimit = arguments?.getInt("filePickerFilesLimit") ?: 10
        val filePickerPhotoSizeLimit =
            arguments?.getLong("filePickerImageMaxSizeInBytes") ?: 10000000L
        val filePickerVideoSizeLimit =
            arguments?.getLong("filePickerVideoMaxSizeInBytes") ?: 10000000L
        val filePickerFileDurationLimit =
            arguments?.getLong("filePickerVideoMaxLengthInSeconds") ?: 10
        val fileLimitPhotoSize = messages["title_image_max_size_limit"] ?: "File is too large"
        val fileLimitVideoSize = messages["title_video_max_size_limit"] ?: "File is too large"
        val fileLimitVideoDuration =
            messages["title_video_max_duration_limit"] ?: "File is too large"

        val translations = mapOf(
            "galleryFileLimitText" to galleryFileLimitText,
            "galleryAccessText" to galleryAccessText,
            "fileLimitPhotoSize" to fileLimitPhotoSize,
            "fileLimitVideoSize" to fileLimitVideoSize,
            "fileLimitVideoDuration" to fileLimitVideoDuration
        )
        previews.load(
            hasFileAccess = hasFileAccess,
            allowMultipleSelection = allowMultiple,
            mimeTypes = acceptTypes,
            clickCallback = object : FileClickCallback {
                override fun select(file: SelectedFile) {
                    selectedFiles.add(file)
                    //selectedFile = filePath
                    uploadButton.show()
                }

                override fun unselect(file: SelectedFile) {
                    selectedFiles.remove(file)
                    if (selectedFiles.isEmpty())
                        uploadButton.hide()
                }
            },
            cameraCallback = object : OpenCameraClickCallback {
                override fun open() {
                    checkCameraPermissions()
                    //openCameraScreen(isVideo)
                }
            },
            storageCallback = object : OpenStorageClickCallback {
                override fun open() {
                    openGooglePhotoPicker()
                    //openCameraScreen(isVideo)
                }
            },
            noAccessCallback = object : NoAccessCallback {
                override fun click() {
                    checkStoragePermissions()
                }
            },
            galleryFileMaxCount = filePickerFilesLimit,
            pickerFilter = PickerFilter(
                filePickerPhotoSizeLimit,
                filePickerVideoSizeLimit,
                1000L * filePickerFileDurationLimit
            ),
            translations = translations
        )
    }

    private fun openSettingsDialog(
        text: String,
        positiveText: String? = null,
        negativeText: String? = null,
        negativeCallback: () -> Unit = {},
    ) {
        if (dialogShown) return
        activity?.apply {
            AlertDialog.Builder(this)
                .setMessage(text)
                .setCancelable(true)
                .setPositiveButton(positiveText ?: "Settings") { dialog, which ->
                    dialog?.dismiss()
                    dialogShown = false
                    openSettingsScreen()
                }
                .setNegativeButton(negativeText ?: "Not now") { dialog, which ->
                    dialog?.dismiss()
                    dialogShown = false
                    negativeCallback.invoke()
                }
                .create()
                .show()
            dialogShown = true
        }
    }

    private fun openSettingsScreen() {
        activity?.apply {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }

    }

    private fun openCameraScreen() {
        if (activity is FileChooseActivity) {
            loaded = false
            (activity as FileChooseActivity).openFileCameraScreen(
                Bundle().also {
                    it.putString(
                        "cameraHint",
                        messages["title_camera_button"] ?: "Tap for photo, hold for video"
                    )
                    it.putInt("contentType", arguments?.getInt("contentType", 0) ?: 0)
                }
            )
        }
    }

}