package com.inappstory.sdk.ugc.camerax

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FileChooseActivity
import com.inappstory.sdk.ugc.utils.faststart.FastStart
import java.io.File

open class PreviewFragment : Fragment() {
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
        val isVideo = filePath.endsWith("mp4")
        val currentFile = File(
            (activity as FileChooseActivity).filesDir,
            if (isVideo) {
                "ugc_video.mp4"
            } else {
                "ugc_photo.jpg"
            }
        )
        if (currentFile.exists()) currentFile.delete()
        val newFile = File(filePath)
        if (newFile.exists()) {
            if (isVideo) {
                val fastStartModule = FastStart(newFile.absolutePath, currentFile.absolutePath)
                if (!fastStartModule.fastStart()) {
                    newFile.renameTo(currentFile)
                }
            } else {
                newFile.renameTo(currentFile)
            }
        }
        (activity as FileChooseActivity).sendResult(
            if (currentFile.exists())
                currentFile.absolutePath
            else
                ""
        )
    }
}