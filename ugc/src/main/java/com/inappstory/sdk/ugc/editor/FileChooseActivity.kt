package com.inappstory.sdk.ugc.editor

import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.picker.FilePicker
import java.io.File
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
        val isVideo = intent.getStringExtra("type").equals("video")
        askPermissions()
        if (savedInstanceState == null) {
            val bundle = Bundle()
            bundle.putBoolean("isVideo", isVideo)
            bundle.putStringArrayList(
                "acceptTypes",
                intent.getStringArrayListExtra("acceptTypes")
            )
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
        currentFragment = CameraFragment().apply {
            bundle.putString("fileName", UUID.randomUUID().toString())
            arguments = bundle
            addFragment(this, "UGC_CAMERA")
        }
    }


    fun openPreviewScreen(bundle: Bundle) {
        currentFragment = VideoPreviewFragment().apply {
            arguments = bundle
            addFragment(this, "UGC_PREVIEW")
        }
    }
}