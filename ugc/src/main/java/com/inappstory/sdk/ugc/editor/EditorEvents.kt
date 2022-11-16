package com.inappstory.sdk.ugc.editor

internal data class SlideAddedEvent(
    var slideIndex: Int? = null,
    var totalSlides: Int? = null,
    var ts: Long? = null
)

internal data class SlideRemovedEvent(
    var slideIndex: Int? = null,
    var totalSlides: Int? = null,
    var ts: Long? = null
)

internal data class StoryPublishedSuccessEvent(
    var totalSlides: Int? = null,
    var ts: Long? = null
)

internal data class StoryPublishedFailEvent(
    var reason: String? = null,
    var totalSlides: Int? = null,
    var ts: Long? = null
)

internal object EditorEventNames {
    const val SLIDE_ADD = "slideAdded"
    const val SLIDE_REMOVE = "slideRemoved"
    const val STORY_PUBLISH = "storyPublishedSuccess"
    const val STORY_PUBLISH_FAIL = "storyPublishedFail"
}
