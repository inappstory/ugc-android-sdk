package com.inappstory.sdk.ugc.camerax

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FileChooseActivity
import com.inappstory.sdk.ugc.utils.faststart.FastStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

open class PreviewFragment : Fragment() {
    lateinit var filePath: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.restart)?.setOnClickListener {
            requireActivity().onBackPressed()
        }
        view.findViewById<View>(R.id.approve)?.setOnClickListener {
            sendResult()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val newFile = File(filePath)
        if (newFile.exists()) newFile.delete()
    }

    private fun sendResult() {
        val isVideo = filePath.endsWith("mp4")
        val dirPath = "${requireActivity().filesDir}/converted"
        val dirAsFile = File(dirPath)
        if (!dirAsFile.exists()) {
            dirAsFile.mkdirs()
        }
        val currentFile = File(
            dirPath,
            if (isVideo) {
                "ugc_video_${System.currentTimeMillis()}.mp4"
            } else {
                "ugc_photo_${System.currentTimeMillis()}.jpg"
            }
        )
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