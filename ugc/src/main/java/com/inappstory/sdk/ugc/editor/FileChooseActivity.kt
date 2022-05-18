package com.inappstory.sdk.ugc.editor

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.inappstory.sdk.ugc.picker.FilePicker

internal class FileChooseActivity : AppCompatActivity() {
    val picker: FilePicker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isVideo = intent.getBooleanExtra("isVideo", false)

    }
}