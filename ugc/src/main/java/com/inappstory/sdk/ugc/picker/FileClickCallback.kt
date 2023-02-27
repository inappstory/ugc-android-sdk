package com.inappstory.sdk.ugc.picker

interface FileClickCallback {
    fun select(filePath: String)

    fun unselect(filePath: String)
}