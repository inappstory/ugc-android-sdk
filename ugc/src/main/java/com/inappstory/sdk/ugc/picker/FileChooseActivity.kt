package com.inappstory.sdk.ugc.picker

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.camerax.CameraXFragment
import com.inappstory.sdk.ugc.camerax.PhotoPreviewFragment
import com.inappstory.sdk.ugc.camerax.VideoPreviewFragment
import java.util.*


internal class FileChooseActivity : AppCompatActivity() {
    val picker: FilePicker? = null

    var currentFragment: Fragment? = null

    @TargetApi(23)
    private fun askPermissions() {
        val readPermission = "android.permission.READ_EXTERNAL_STORAGE"
        val writePermission = "android.permission.WRITE_EXTERNAL_STORAGE"
        val permissions = arrayListOf<String>()
        if (ActivityCompat.checkSelfPermission(this, readPermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(readPermission)
        }
        if (ActivityCompat.checkSelfPermission(this, writePermission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            permissions.add(writePermission)
        }
        if (permissions.isNotEmpty())
            requestPermissions(permissions.toTypedArray(), 200)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cs_file_choose_activity)

        // askPermissions()
        if (savedInstanceState == null) {
            val bundle = Bundle().apply {
                putStringArrayList(
                    "acceptTypes",
                    intent.getStringArrayListExtra("acceptTypes")
                )
                putStringArray(
                    "messageNames",
                    intent.getStringArrayExtra("messageNames")
                )
                putBoolean(
                    "allowMultiple",
                    intent.getBooleanExtra("allowMultiple", false)
                )
                putInt(
                    "filePickerFilesLimit",
                    intent.getIntExtra("filePickerFilesLimit", 10)
                )
                putStringArray(
                    "messages",
                    intent.getStringArrayExtra("messages")
                )
            }
            openFilePickerScreen(bundle)
        }
    }

    private fun openFragment(fragment: Fragment, tag: String) {
        try {
            val fragmentManager =
                supportFragmentManager
            val t = fragmentManager.beginTransaction()
                .replace(R.id.fragments_layout, fragment, tag)
            t.addToBackStack(null)
            t.commitAllowingStateLoss()
        } catch (e: IllegalStateException) {
            finish()
        }
    }

    private fun addFragment(fragment: Fragment, tag: String) {
        try {
            val fragmentManager =
                supportFragmentManager
            val t = fragmentManager.beginTransaction()
                .add(R.id.fragments_layout, fragment, tag)
            t.addToBackStack(null)
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
        intent.putExtra("files", arrayOf(filePath))
        setResult(RESULT_OK, intent)
        finish()
    }

    fun sendResultMultiple(files: Array<String>) {
        val intent = Intent()
        intent.putExtra("files", files)
        setResult(RESULT_OK, intent)
        finish()
    }

    fun openFilePickerScreen(bundle: Bundle) {
        currentFragment = FilePickerFragment().apply {
            arguments = bundle
            openFragment(this, "UGC_FILE_CHOOSE")
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val fragment = supportFragmentManager.findFragmentByTag("UGC_FILE_CHOOSE")
        if (fragment is FilePickerFragment) {
            fragment.requestPermissionsResult(requestCode, permissions, grantResults)
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun openFileCameraScreen(bundle: Bundle) {
        currentFragment = CameraXFragment().apply {
            bundle.putString("fileName", UUID.randomUUID().toString())
            arguments = bundle
            addFragment(this, "UGC_CAMERA")
        }
    }

    private val cache = FilePreviewsCache(true)

    fun loadPreview(path: String, imageView: ImageView, isVideo: Boolean) {
        cache.loadPreview(path, imageView, isVideo)
    }

    fun openPreviewScreen(isVideo: Boolean, filePath: String) {
        currentFragment = (if (isVideo) {
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