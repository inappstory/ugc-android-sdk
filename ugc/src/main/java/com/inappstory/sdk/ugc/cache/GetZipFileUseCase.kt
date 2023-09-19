package com.inappstory.sdk.ugc.cache

import androidx.annotation.WorkerThread
import com.inappstory.sdk.InAppStoryService
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
        val cache = inAppStoryService.infiniteCache
        GetLocalZipFileUseCase(
            url = url
        ).get(
            callback = object : UseCaseCallback<File> {
                override fun onError(message: String?) {
                    val hash = UUID.randomUUID().toString()
                    val zipName = getZipName(url)
                    val gameDir = File(
                        cache.cacheDir.toString() +
                                File.separator + "zip" +
                                File.separator + zipName +
                                File.separator
                    )
                    if (!gameDir.absolutePath.startsWith(
                            cache.cacheDir.toString() +
                                    File.separator + "zip"
                        )
                    ) {
                        callback.onError("Error in ugc editor name")
                        return
                    }
                    val zipFile = File(gameDir, url.hashCode().toString() + ".zip")
                    var fileState: DownloadFileState? = null
                    ProfilingManager.getInstance().addTask("ugc_download", hash)
                    try {
                        fileState = Downloader.downloadOrGetFile(
                            url,
                            false,
                            InAppStoryService.getInstance().infiniteCache,
                            zipFile,
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
                            hash
                        )
                    } catch (e: Exception) {
                        callback.onError(e.message)
                    }
                    if (fileState?.file != null && fileState.file.exists()
                        && fileState.downloadedSize == fileState.totalSize
                    ) {
                        callback.onSuccess(fileState.file)
                        ProfilingManager.getInstance().setReady(hash)
                    } else {
                        callback.onError("File downloading was interrupted")
                    }
                }

                override fun onSuccess(result: File) {
                    callback.onSuccess(result)
                }
            },
            cache = cache
        )
    }
}