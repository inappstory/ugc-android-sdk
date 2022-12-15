package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

data class PickerFilter(val selection: String, val selectionArgs: Array<String>) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PickerFilter

        if (selection != other.selection) return false
        if (!selectionArgs.contentEquals(other.selectionArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = selection.hashCode()
        result = 31 * result + selectionArgs.contentHashCode()
        return result
    }
}

abstract class FilePicker {
    abstract fun openCamera(context: Context)

    abstract fun getImagesPath(context: Context, mimeTypes: List<String>?): ArrayList<String>?

    abstract fun getFilter(): PickerFilter?

    protected fun getImagesPath(
        context: Context,
        uri: Uri?,
        mimeTypes: List<String>?
    ): ArrayList<String> {
        val listOfAllImages = ArrayList<String>()
        val cursor: Cursor?
        val projection = arrayOf(
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.MediaColumns.DURATION
            } else {
                MediaStore.Video.VideoColumns.DURATION
            },
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        val filter = getFilter()
        cursor = context.contentResolver.query(
            uri!!,
            projection,
            filter?.selection,
            filter?.selectionArgs,
            "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"
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