package com.inappstory.sdk.ugc.camerax

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FileChooseActivity

open class PreviewFragment: Fragment() {
    lateinit var filePath: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.restart)?.setOnClickListener {
            activity?.onBackPressed()
        }
        view.findViewById<View>(R.id.approve)?.setOnClickListener {
            sendResult()
        }
    }

    private fun sendResult() {
        (activity as FileChooseActivity).sendResult(filePath)
    }
}