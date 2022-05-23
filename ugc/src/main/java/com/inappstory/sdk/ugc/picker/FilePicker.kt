package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import java.util.ArrayList

abstract class FilePicker {
    abstract fun openCamera(context: Context)

    abstract fun getImagesPath(context: Context, mimeTypes: List<String>?): ArrayList<String>?

    private val columnID = "_id"

    protected fun getImagesPath(
        context: Context,
        uri: Uri?,
        mimeTypes: List<String>?
    ): ArrayList<String> {
        val listOfAllImages = ArrayList<String>()
        val cursor: Cursor?
        var pathOfImage: String? = null
        var mt: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA, MediaStore.MediaColumns.MIME_TYPE)
        val selection: String? = null
        /* var filter: MutableList<String>? = null
    if (mimeTypes != null) {
        selection = ""
        filter = mutableListOf()
    }*/
        cursor = context.contentResolver.query(
            uri!!, projection, selection,
            null, null
        )
        val columnIndexData: Int = cursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
        val columnIndexMT: Int = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
        while (cursor.moveToNext()) {
            if (mimeTypes != null && mimeTypes.contains(cursor.getString(columnIndexMT))) {
                listOfAllImages.add(cursor.getString(columnIndexData))
            }
        }
        cursor.close()
        return listOfAllImages
    }
}