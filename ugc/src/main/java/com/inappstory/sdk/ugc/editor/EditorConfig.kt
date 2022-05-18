package com.inappstory.sdk.ugc.editor

import com.inappstory.sdk.stories.api.models.SessionEditorConfig

internal class EditorConfig {
    var sessionId: String? = null
    var config: SessionEditorConfig? = null
    var isSandbox = false
    var tId: String? = null
    var sdkVersion: String? = null
    var storyId: Int? = null
    var title: String? = null
    var cover: String? = null
}