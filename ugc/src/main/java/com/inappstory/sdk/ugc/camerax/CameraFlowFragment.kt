package com.inappstory.sdk.ugc.camerax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.inappstory.sdk.ugc.R
import java.io.File

class CameraFlowFragment : BackPressedFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.camera_flow_fragment, null)
    }


    private fun openFragment(fragment: Fragment, tag: String, addToBackStack: Boolean = true) {
        try {
            val fragmentManager =
                childFragmentManager
            val t = fragmentManager.beginTransaction()
                .replace(R.id.fragments_layout, fragment, tag)
            if (addToBackStack)
                t.addToBackStack(tag)
            t.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            activity?.onBackPressed()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val contentType = arguments?.getInt("contentType", 0) ?: 0
        val cameraHint = arguments?.getString("cameraHint")
        openFragment(fragment = CameraXFragment().apply {
            arguments = Bundle().apply {
                putInt("contentType", contentType)
                putString("cameraHint", cameraHint)
            }
        }, tag = "UGC_CAMERA_X", addToBackStack = false)
    }


    private val cachedFiles = arrayListOf<String>()
    private var latestFile: String? = null

    override fun onDestroy() {
        clearCachedFiles()
        super.onDestroy()
    }

    private fun clearCachedFiles() {
        cachedFiles.forEach {
            val file = File(it)
            if (file.exists()) file.delete()
        }
    }

    fun openPreviewScreen(isVideo: Boolean, filePath: String) {
        latestFile = filePath
        cachedFiles.add(filePath)
        (if (isVideo) {
            VideoPreviewFragment()
        } else {
            PhotoPreviewFragment()
        }).apply {
            arguments = Bundle().apply {
                putString("filePath", filePath)
            }
            openFragment(this, "UGC_PREVIEW")
        }
    }
}