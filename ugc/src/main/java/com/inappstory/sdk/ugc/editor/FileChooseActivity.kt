package com.inappstory.sdk.ugc.editor

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.inappstory.sdk.InAppStoryService
import com.inappstory.sdk.stories.ui.reader.StoriesFragment
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FilePicker
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.lang.IllegalStateException
import android.content.Intent
import java.util.*


internal class FileChooseActivity : AppCompatActivity() {
    val picker: FilePicker? = null

    var currentFragment: Fragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cs_file_choose_activity)
        val isVideo = intent.getStringExtra("type").equals("video")
        val acceptTypes = arrayListOf<String>()

        if (savedInstanceState == null) {
            val bundle = Bundle()
            bundle.putBoolean("isVideo", isVideo)
            bundle.putStringArrayList("acceptTypes",
                intent.getStringArrayListExtra("acceptTypes"))
            openFilePickerScreen(bundle)
        }/* else {
            currentFragment =
                supportFragmentManager.findFragmentById(R.id.fragments_layout)
        }*/
    }

    fun openFragment(fragment: Fragment, tag: String) {
        try {
            val fragmentManager =
                supportFragmentManager
            val t = fragmentManager.beginTransaction()
                .replace(R.id.fragments_layout, fragment)
            t.addToBackStack(tag)
            t.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            finish()
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1)
            finish()
        else
            super.onBackPressed()
    }

    fun sendResult(filePath: String) {
        val intent = Intent()
        intent.putExtra("file", filePath)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun openFilePickerScreen(bundle: Bundle) {
        currentFragment = FilePickerFragment().apply {
            arguments = bundle
            openFragment(this, "UGC_FILE_CHOOSE")
        }
    }

    fun openFileCameraScreen(bundle: Bundle) {
        currentFragment = CameraFragment().apply {
            bundle.putString("fileName", UUID.randomUUID().toString())
            arguments = bundle
            openFragment(this, "UGC_CAMERA")
        }
    }


    fun openPreviewScreen(bundle: Bundle) {
        currentFragment = VideoPreviewFragment().apply {
            arguments = bundle
            openFragment(this, "UGC_PREVIEW")
        }
    }
}