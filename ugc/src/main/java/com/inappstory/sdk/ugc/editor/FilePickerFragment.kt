package com.inappstory.sdk.ugc.editor

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FileClickCallback
import com.inappstory.sdk.ugc.picker.FilePreviewsList
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
                loadPreviews()
            }
        }
    }

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
        val appPerms = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        )
        activityResultLauncher.launch(appPerms)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun loadPreviews() {
        previews?.load(isVideo, acceptTypes, object : FileClickCallback {
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
                if (activity is FileChooseActivity) {
                    (activity as FileChooseActivity).openFileCameraScreen(
                        Bundle().also {
                            it.putBoolean("isVideo", isVideo)
                        }
                    )
                }
            }
        })
    }

}