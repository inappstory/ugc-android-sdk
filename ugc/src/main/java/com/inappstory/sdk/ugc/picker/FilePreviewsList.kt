package com.inappstory.sdk.ugc.picker

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class FilePreviewsList @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(context, attrs, defStyle) {

    init {
        layoutManager = GridLayoutManager(
            context, 3,
            GridLayoutManager.VERTICAL,
            false
        )
    }

    fun load(
        hasFileAccess: Boolean,
        allowMultipleSelection: Boolean,
        mimeTypes: List<String>,
        clickCallback: FileClickCallback,
        cameraCallback: OpenCameraClickCallback,
        noAccessCallback: NoAccessCallback,
        galleryAccessText: String,
        galleryFileMaxCount: Int,
        galleryFileLimitText: String,
        pickerFilter: PickerFilter
    ) {
        val adapter = FilePreviewsAdapter(
            context,
            hasFileAccess,
            allowMultipleSelection,
            mimeTypes,
            clickCallback,
            cameraCallback,
            noAccessCallback,
            galleryAccessText,
            galleryFileMaxCount,
            galleryFileLimitText,
            pickerFilter
        )
        adapter.setHasStableIds(true)
        setAdapter(adapter)
    }
}