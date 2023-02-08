package com.inappstory.sdk.ugc.editor

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FileClickCallback
import com.inappstory.sdk.ugc.picker.FilePreviewsList
import com.inappstory.sdk.ugc.picker.NoAccessCallback
import com.inappstory.sdk.ugc.picker.OpenCameraClickCallback


internal class FilePickerFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.cs_file_picker_fragment, null)
    }

    private var uploadButton: FloatingActionButton? = null
    private var previews: FilePreviewsList? = null

    var isVideo = false
    var acceptTypes: ArrayList<String>? = null
    var selectedFile: String? = null

    private val STORAGE_PERMISSIONS_RESULT = 888
    private val CAMERA_PERMISSIONS_RESULT_PHOTO = 889
    private val CAMERA_PERMISSIONS_RESULT_VIDEO = 890

    private fun checkStoragePermissions() {
        activity?.apply {
            var allGranted = true;
            val localPerms = arrayListOf<String>()
            appPerms.forEach {
                if (ContextCompat.checkSelfPermission(
                        this,
                        it
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    allGranted = false;
                    localPerms.add(it)
                }
            }
            if (!allGranted) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    this.requestPermissions(localPerms.toTypedArray(), STORAGE_PERMISSIONS_RESULT)
                }
            } else {
                if (!loaded)
                    loadPreviews(true)
            }
        }
    }

    private fun checkCameraPermissions(isVideo: Boolean) {
        activity?.apply {
            var allGranted = true;
            val localPerms = arrayListOf(Manifest.permission.CAMERA).apply {
                if (isVideo)
                    add(Manifest.permission.RECORD_AUDIO)
            }
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
                        if (isVideo) CAMERA_PERMISSIONS_RESULT_VIDEO else CAMERA_PERMISSIONS_RESULT_PHOTO
                    )
                }
            } else {
                openCameraScreen(isVideo)
            }
        }
    }

    private var loaded = false
    private var dialogShown = false

    private val appPerms = arrayOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireArguments().getBoolean("isVideo", false)
        uploadButton = view.findViewById(R.id.upload)
        previews = view.findViewById(R.id.previews)
        isVideo = requireArguments().getBoolean("isVideo", false)
        acceptTypes = requireArguments().getStringArrayList("acceptTypes")
        if (acceptTypes == null) {
            activity?.onBackPressed()
            return
        }
        uploadButton?.setOnClickListener {
            if (activity is FileChooseActivity && selectedFile != null) {
                (activity as FileChooseActivity).sendResult(selectedFile!!)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        checkStoragePermissions()
    }

    fun requestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        var allGranted = true;
        if (requestCode == STORAGE_PERMISSIONS_RESULT
            || requestCode == CAMERA_PERMISSIONS_RESULT_PHOTO
            || requestCode == CAMERA_PERMISSIONS_RESULT_VIDEO
        ) {
            if (grantResults.isNotEmpty()) {
                permissions.forEachIndexed { index, permission ->
                    if (grantResults[index] != 0)
                        allGranted = false;
                }
            } else {
                allGranted = false;
            }
            when (requestCode) {
                STORAGE_PERMISSIONS_RESULT -> {
                    if (!allGranted)
                        openSettingsDialog("You need storage access to load photos and videos. Tap Settings > Permissions and turn \'Files and media\' on") {
                            loadPreviews(false)
                        }
                    else
                        loadPreviews(true)
                }
                CAMERA_PERMISSIONS_RESULT_PHOTO -> {
                    if (!allGranted)
                        openSettingsDialog("You need camera access to make photos. Tap Settings > Permissions and turn \'Camera\' on")
                    else
                        openCameraScreen(false)
                }
                CAMERA_PERMISSIONS_RESULT_VIDEO -> {
                    if (!allGranted)
                        openSettingsDialog("You need camera and microphone access to make videos. Tap Settings > Permissions and turn \'Camera\' and \'Microphone\' on")
                    else
                        openCameraScreen(true)
                }
            }
        }
    }

    private fun loadPreviews(hasFileAccess: Boolean) {
        loaded = hasFileAccess
        previews?.load(isVideo, hasFileAccess, acceptTypes, object : FileClickCallback {
            override fun select(filePath: String) {
                selectedFile = filePath
                uploadButton?.show()
            }

            override fun unselect() {
                selectedFile = null
                uploadButton?.hide()
            }
        }, object : OpenCameraClickCallback {
            override fun open(isVideo: Boolean) {
                checkCameraPermissions(isVideo)
                //openCameraScreen(isVideo)
            }
        }, object : NoAccessCallback {
            override fun click() {
                checkStoragePermissions()
            }
        })
    }

    private fun openSettingsDialog(text: String, negativeCallback: () -> Unit = {}) {
        if (dialogShown) return
        activity?.apply {
            AlertDialog.Builder(this)
                .setMessage(text)
                .setCancelable(true)
                .setPositiveButton("Settings") { dialog, which ->
                    dialog?.dismiss()
                    dialogShown = false
                    openSettingsScreen()
                }
                .setNegativeButton("Not now") { dialog, which ->
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

    private fun openCameraScreen(isVideo: Boolean) {
        if (activity is FileChooseActivity) {
            loaded = false
            (activity as FileChooseActivity).openFileCameraScreen(
                Bundle().also {
                    it.putBoolean("isVideo", isVideo)
                }
            )
        }
    }

}