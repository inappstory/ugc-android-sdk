package com.inappstory.sdk.ugc.camerax

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.inappstory.sdk.ugc.R

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

    fun openPreviewScreen(isVideo: Boolean, filePath: String) {
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