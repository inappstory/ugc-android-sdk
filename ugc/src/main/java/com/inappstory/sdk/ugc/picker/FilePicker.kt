package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull

data class PickerFilter(val imageSize: Long, val videoSize: Long, val duration: Long)

data class UriAndType(val uri: Uri, val type: String)

abstract class FilePicker {
    abstract fun openCamera(context: Context)

    data class FileData(val name: String, val duration: Long? = null, val date: Long)

    abstract fun getImagesPath(
        context: Context,
        pickerFilter: PickerFilter,
        mimeTypes: List<String>
    ): List<FileData>

    protected fun getImagesPath(
        context: Context,
        uri: List<UriAndType>,
        pickerFilter: PickerFilter,
        mimeTypes: List<String>
    ): List<FileData> {
        val listOfAllImages = ArrayList<FileData>()
        val durationColumn =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.MediaColumns.DURATION
            } else {
                MediaStore.Video.VideoColumns.DURATION
            }
        val projection = arrayOf(
            MediaStore.MediaColumns.TITLE,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.MIME_TYPE,
            durationColumn,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.DATE_MODIFIED
        )
        uri.forEach {
            val mergeCursor = context.contentResolver.query(
                it.uri,
                projection,
                null,
                null,
                null
            )

            val fileFilterSize =
                if (it.type == "video") pickerFilter.videoSize else pickerFilter.imageSize
            val columnIndexDate: Int =
                mergeCursor!!.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED)
            val columnIndexSize: Int =
                mergeCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)

            val columnIndexData: Int =
                mergeCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
            val columnIndexDuration: Int = mergeCursor.getColumnIndexOrThrow(durationColumn)
            val columnIndexMT: Int =
                mergeCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            while (mergeCursor.moveToNext()) {
                if (mimeTypes.contains(mergeCursor.getString(columnIndexMT))) {
                    val duration = mergeCursor.getLongOrNull(columnIndexDuration)
                    val size = mergeCursor.getLongOrNull(columnIndexSize)
                    if ((fileFilterSize >= (size ?: 0L)) && (pickerFilter.duration >= (duration
                            ?: 0L))
                    ) {
                        listOfAllImages.add(
                            FileData(
                                mergeCursor.getString(columnIndexData),
                                duration,
                                mergeCursor.getLong(columnIndexDate),
                            )
                        )
                    }
                }
            }
            mergeCursor.close()
        }
        return listOfAllImages.sortedByDescending { it.date }
    }
}