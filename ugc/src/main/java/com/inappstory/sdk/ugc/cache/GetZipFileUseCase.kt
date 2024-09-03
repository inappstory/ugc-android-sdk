package com.inappstory.sdk.ugc.cache

import androidx.annotation.WorkerThread
import com.inappstory.sdk.InAppStoryService
import com.inappstory.sdk.UseServiceInstanceCallback
import com.inappstory.sdk.stories.cache.DownloadFileState
import com.inappstory.sdk.stories.cache.DownloadInterruption
import com.inappstory.sdk.stories.cache.Downloader
import com.inappstory.sdk.stories.cache.FileLoadProgressCallback
import com.inappstory.sdk.stories.statistic.ProfilingManager
import java.io.File
import java.util.*

class GetZipFileUseCase(
    var url: String
) : ZipNameHolder() {

    @WorkerThread
    fun get(
        interruption: DownloadInterruption?,
        callback: UseCaseCallback<File>,
        progressCallback: ProgressCallback
    ) {
        val inAppStoryService = InAppStoryService.getInstance()
        if (inAppStoryService == null) {
            callback.onError("InAppStory service is unavailable")
            return
        }
        UgcArchiveUseCase(
            inAppStoryService.filesDownloadManager,
            url,
            object : FileLoadProgressCallback {
                override fun onProgress(loadedSize: Long, totalSize: Long) {
                    progressCallback.onProgress(loadedSize, totalSize)
                }

                override fun onSuccess(file: File) {}
                override fun onError(error: String) {
                    callback.onError(error)
                }
            },
            interruption,
            callback
        ).getFile()
    }
}