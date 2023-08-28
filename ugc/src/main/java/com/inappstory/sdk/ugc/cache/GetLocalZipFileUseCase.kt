package com.inappstory.sdk.ugc.cache

import com.inappstory.sdk.lrudiskcache.LruDiskCache
import java.io.File

class GetLocalZipFileUseCase(var url: String) {
    fun get(
        callback: UseCaseCallback<File>,
        cache: LruDiskCache
    ): Boolean {
        val cachedArchive = cache.getFullFile(url)
        return if (cachedArchive != null && cachedArchive.exists()) {
            callback.onSuccess(cachedArchive)
            true
        } else {
            callback.onError("No cached files")
            false
        }
    }
}