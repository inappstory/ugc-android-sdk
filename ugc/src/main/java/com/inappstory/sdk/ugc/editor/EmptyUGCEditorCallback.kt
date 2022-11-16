package com.inappstory.sdk.ugc.editor

import com.inappstory.sdk.ugc.UGCEditorCallback

internal class EmptyUGCEditorCallback : UGCEditorCallback {

    override fun editorEvent(eventName: String, eventData: HashMap<String, Any?>?) {
    }

}