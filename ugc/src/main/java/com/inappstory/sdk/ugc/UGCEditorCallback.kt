package com.inappstory.sdk.ugc

interface UGCEditorCallback {
    fun editorEvent(eventName: String, eventData: HashMap<String, Any?>? = null)
}