package com.inappstory.sdk.ugc.cache

import com.inappstory.sdk.lrudiskcache.LruDiskCache
import java.io.File

class GetLocalZipFileUseCase(var uniqueKey: String) {
    fun get(
        callback: UseCaseCallback<File>,
        cache: LruDiskCache
    ): Boolean {
        val cachedArchive = cache.getFullFile(uniqueKey)
        return if (cachedArchive != null && cachedArchive.exists()) {
            callback.onSuccess(cachedArchive)
            true
        } else {
            false
        }
    }
}