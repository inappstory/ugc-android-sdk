package com.inappstory.sdk.ugc

import android.content.Context
import android.view.View

interface IUGCReaderLoaderView {
    fun getView(context: Context): View

    fun setProgress(progress: Int, max: Int)

    fun setIndeterminate(indeterminate: Boolean)
}