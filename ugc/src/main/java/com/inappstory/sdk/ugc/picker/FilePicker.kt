package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.util.ArrayList

abstract class FilePicker {
    abstract fun openCamera(context: Context)

    abstract fun getImagesPath(context: Context, mimeTypes: List<String>?): ArrayList<String>?

    protected fun getImagesPath(
        context: Context,
        uri: Uri?,
        mimeTypes: List<String>?
    ): ArrayList<String> {
        val listOfAllImages = ArrayList<String>()
        val cursor: Cursor?
        var pathOfImage: String? = null
        val projection = arrayOf(MediaStore.Images.Media._ID)
        var selection: String? = null
        var filter: MutableList<String>? = null
        if (mimeTypes != null) {
            selection = ""
            filter = mutableListOf()
        }
        cursor = context.contentResolver.query(
            uri!!, projection, selection,
            filter?.toTypedArray(), null
        )
        val columnIndexData: Int = cursor!!.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
        while (cursor.moveToNext()) {
            pathOfImage = cursor.getString(columnIndexData)
            listOfAllImages.add(pathOfImage)
        }
        cursor.close()
        return listOfAllImages
    }
}