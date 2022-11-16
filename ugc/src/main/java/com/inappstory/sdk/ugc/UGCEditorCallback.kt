package com.inappstory.sdk.ugc

interface UGCEditorCallback {
    fun editorWillShow()
    fun editorDidClose()
    fun slideAdded(slideIndex: Int?, totalSlides: Int?, ts: Long?)
    fun slideRemoved(slideIndex: Int?, totalSlides: Int?, ts: Long?)
    fun storyPublishedSuccess(totalSlides: Int?, ts: Long?)
    fun storyPublishedFail(reason: String?, totalSlides: Int?, ts: Long?)
}