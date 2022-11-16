package com.inappstory.sdk.ugc.editor

import android.util.Log
import com.inappstory.sdk.ugc.UGCEditorCallback

internal class EmptyUGCEditorCallback : UGCEditorCallback {
    override fun editorWillShow() {
        Log.d("EmptyUGCEditorCallback", "editorWillShow")
    }

    override fun editorDidClose() {
        Log.d("EmptyUGCEditorCallback", "editorDidClose")
    }

    override fun slideAdded(slideIndex: Int?, totalSlides: Int?, ts: Long?) {
        Log.d("EmptyUGCEditorCallback", "slideAdded $slideIndex $totalSlides $ts")
    }

    override fun slideRemoved(slideIndex: Int?, totalSlides: Int?, ts: Long?) {
        Log.d("EmptyUGCEditorCallback", "slideRemoved $slideIndex $totalSlides $ts")
    }

    override fun storyPublishedSuccess(totalSlides: Int?, ts: Long?) {
        Log.d("EmptyUGCEditorCallback", "storyPublishedSuccess $totalSlides $ts")
    }

    override fun storyPublishedFail(reason: String?, totalSlides: Int?, ts: Long?) {
        Log.d("EmptyUGCEditorCallback", "storyPublishedFail $reason $totalSlides $ts")
    }
}