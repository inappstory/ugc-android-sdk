package com.inappstory.sdk.ugc.picker

interface FileClickCallback {
    fun select(file: SelectedFile)

    fun unselect(file: SelectedFile)
}