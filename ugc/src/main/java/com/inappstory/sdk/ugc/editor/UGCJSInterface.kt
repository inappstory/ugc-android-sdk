package com.inappstory.sdk.ugc.editor

import android.webkit.JavascriptInterface
import com.inappstory.sdk.network.JsonParser

internal class UGCJSInterface(val editor: UGCEditor) {
    @JavascriptInterface
    fun editorLoaded(data: String) {
        editor.ugcLoaded = true
        val config = JsonParser.fromJson(
            data,
            EditorLoadedResult::class.java
        )
        editor.handleBack = config.backHandler ?: false
        editor.updateUI()
    }

    @JavascriptInterface
    fun closeEditor() {
        editor.finish()
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
}