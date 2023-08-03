package com.inappstory.sdk.ugc.editor

import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface

internal class UGCJSInterface(val editor: UGCEditor) {
    @JavascriptInterface
    fun editorLoaded(data: String) {
        editor.editorLoaded(data)
    }

    @JavascriptInterface
    fun closeEditor() {
        editor.closeEditor()
    }

    @JavascriptInterface
    fun sendApiRequest(data: String?) {
        editor.sendApiRequest(data)
    }

    @JavascriptInterface
    fun editorEvent(event: String, payload: String) {
        event.let {
            editor.sendEditorEvent(it, payload)
        }
    }

    @JavascriptInterface
    fun openFilePicker(data: String) {
        editor.openFilePicker(data)
    }
}