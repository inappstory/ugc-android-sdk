package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.provider.MediaStore
import java.util.ArrayList

internal class VideoPicker : FilePicker() {
    override fun openCamera(context: Context) {}

    override fun getImagesPath(context: Context, mimeTypes: List<String>?): ArrayList<String> {
        val path = ArrayList<String>()
        path.addAll(
            getImagesPath(
                context,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                mimeTypes
            )
        )
        path.addAll(
            getImagesPath(
                context,
                MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                mimeTypes
            )
        )
        return path
    }

    override fun getFilter(): PickerFilter? {
        val lengthLimit = 30000
        val sizeLimit = 30000000
        return PickerFilter(
            selection = "${
                MediaStore.MediaColumns.SIZE
            } <= ? AND ${
                MediaStore.Video.VideoColumns.DURATION
            } <= ?",
            selectionArgs = arrayOf("$sizeLimit", "$lengthLimit")
        )
    }
}