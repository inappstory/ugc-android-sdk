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
        val projection = arrayOf(
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        cursor = context.contentResolver.query(
            uri!!, projection, null,
            null, "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
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