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
        galleryFileMaxCount: Int,
        translations: Map<String, String>,
        pickerFilter: PickerFilter
    ) {
        val adapter = FilePreviewsAdapter(
            context = context,
            hasFileAccess = hasFileAccess,
            allowMultipleSelection = allowMultipleSelection,
            mimeTypes = mimeTypes,
            clickCallback = clickCallback,
            cameraCallback = cameraCallback,
            noAccessCallback = noAccessCallback,
            galleryFileMaxCount = galleryFileMaxCount,
            pickerFilter = pickerFilter,
            translations = translations
        )
        adapter.setHasStableIds(true)
        setAdapter(adapter)
    }
}