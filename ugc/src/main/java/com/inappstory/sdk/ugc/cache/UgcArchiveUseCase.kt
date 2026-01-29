package com.inappstory.sdk.ugc.cache

import com.inappstory.sdk.core.IASCore
import com.inappstory.sdk.lrudiskcache.CacheJournalItem
import com.inappstory.sdk.lrudiskcache.LruDiskCache
import com.inappstory.sdk.stories.cache.DownloadInterruption
import com.inappstory.sdk.stories.cache.FileLoadProgressCallback
import com.inappstory.sdk.stories.cache.FilesDownloader
import com.inappstory.sdk.stories.cache.usecases.FinishDownloadFileCallback
import com.inappstory.sdk.stories.cache.usecases.GetCacheFileUseCase
import com.inappstory.sdk.utils.ProgressCallback
import com.inappstory.sdk.utils.StringsUtils
import java.io.File
import java.io.IOException
import java.util.UUID

class UgcArchiveUseCase(
    private val core: IASCore,
    private val url: String,
    private val progressCallback: ProgressCallback,
    private val interruption: DownloadInterruption?,
    private val useCaseCallback: UseCaseCallback<File>
) : GetCacheFileUseCase<Unit>(
    core
) {
    private val type: String = "Archive"
    private val archiveName = getArchiveName(url)

    init {
        this.uniqueKey = StringsUtils.md5(url)
        this.filePath = cache.cacheDir.absolutePath +
                File.separator +
                "v2" +
                File.separator +
                "zip" +
                File.separator +
                archiveName +
                File.separator +
                uniqueKey +
                FilesDownloader.getFileExtensionFromUrl(url);
    }

    private fun getLocalArchive(): Boolean {
        downloadLog.generateRequestLog(url)
        return GetLocalZipFileUseCase(
            uniqueKey = uniqueKey
        ).get(
            useCaseCallback,
            cache
        ).apply {
            if (this) {
                downloadLog.generateResponseLog(true, filePath)
                downloadLog.sendRequestResponseLog()
            }
        }
    }

    private fun downloadArchive() {
        downloadLog.sendRequestLog()
        if (!filePath.startsWith(
                cache.cacheDir.absolutePath +
                        File.separator +
                        "v2" +
                        File.separator +
                        "zip"
            )
        ) {
            useCaseCallback.onError("Error in ugc editor name")
            return
        }
        val hash = UUID.randomUUID().toString()
        core.statistic().profiling().addTask("ugc_download", hash)
        try {

            val responseLog = downloadLog.generateResponseLog(false, filePath)
            val callback =
                FinishDownloadFileCallback { fileState ->
                    downloadLog.sendResponseLog()
                    if (fileState?.file != null) {
                        core.statistic().profiling().setReady(hash)
                        val cacheJournalItem = generateCacheItem()
                        cacheJournalItem.downloadedSize = fileState.downloadedSize
                        cacheJournalItem.size = fileState.totalSize
                        try {
                            cache.put(cacheJournalItem, type)
                        } catch (ignored: IOException) {
                        }
                        useCaseCallback.onSuccess(fileState.file)
                    } else {
                        useCaseCallback.onError("File downloading was interrupted")
                    }
                }
            var offset = 0L
            cache[uniqueKey]?.also {
                offset = it.downloadedSize
            }
            core.contentLoader().downloader().downloadFile(
                url,
                File(filePath),
                object : FileLoadProgressCallback {
                    override fun onProgress(loadedSize: Long, totalSize: Long) {
                        progressCallback.onProgress(loadedSize, totalSize)
                    }

                    override fun onSuccess(file: File) {}
                    override fun onError(error: String) {
                        useCaseCallback.onError(error)
                    }
                },
                responseLog,
                interruption,
                offset,
                -1,
                callback
            )
        } catch (e: Exception) {

            useCaseCallback.onError(e.message)
        }
    }

    override fun getFile() {
        if (!getLocalArchive())
            downloadArchive()
    }

    private fun getArchiveName(url: String): String {
        val parts = url.split("/".toRegex())
        val fName = parts[parts.size - 1].split("\\.".toRegex())[0]
        val nameParts = fName.split("_".toRegex())
        return if (nameParts.isNotEmpty()) nameParts[0] else ""
    }

    override fun generateCacheItem(): CacheJournalItem {
        return CacheJournalItem(
            uniqueKey,
            filePath,
            null,
            type,
            null,
            null,
            System.currentTimeMillis(),
            0,
            0,
            null
        );
    }

    override fun getCache(): LruDiskCache {
        return core.contentLoader().infiniteCache
    }
}