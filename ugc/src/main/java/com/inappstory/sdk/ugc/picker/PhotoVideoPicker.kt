package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.provider.MediaStore
import java.util.ArrayList

internal class PhotoVideoPicker : FilePicker() {
    override fun openCamera(context: Context) {}

    override fun getImagesPath(
        context: Context,
        pickerFilter: PickerFilter,
        mimeTypes: List<String>?
    ): List<FileData> {
        return getImagesPath(
            context = context,
            uri = listOf(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Video.Media.INTERNAL_CONTENT_URI,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Images.Media.INTERNAL_CONTENT_URI
            ),
            pickerFilter = pickerFilter,
            mimeTypes = mimeTypes
        )
    }
}