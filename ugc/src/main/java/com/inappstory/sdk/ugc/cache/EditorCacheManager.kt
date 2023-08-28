package com.inappstory.sdk.ugc.cache

import com.inappstory.sdk.InAppStoryService
import com.inappstory.sdk.stories.cache.DownloadInterruption
import java.io.File

class EditorCacheManager {
    suspend fun getEditor(
        resultCallback: UseCaseCallback<FilePathAndContent>,
        progressCallback: ProgressCallback,
        editorUrl: String,
        interruption: DownloadInterruption
    ) {
        val inAppStoryService = InAppStoryService.getInstance()
        if (inAppStoryService == null) {
            resultCallback.onError("InAppStory service is unavailable")
            return
        }
        val fileManager = FileManager()
        val unzippedFileCallback = object : UseCaseCallback<String> {
            override fun onError(message: String?) {

            }

            override fun onSuccess(directoryStr: String) {
                val fileName: String = directoryStr + File.separator + "index.html"
                resultCallback.onSuccess(
                    FilePathAndContent(
                        filePath = "file://$fileName",
                        fileContent = fileManager.getStringFromFile(File(fileName))
                    )
                )
            }
        }
        GetZipFileUseCase(url = editorUrl).get(
            interruption = interruption,
            callback = object : UseCaseCallback<File> {
                override fun onError(message: String?) {
                    resultCallback.onError(message)
                }

                override fun onSuccess(result: File) {
                    val directory = File(
                        (result.parent?.plus(File.separator) ?: "") +
                                editorUrl.hashCode()
                    )
                    if (directory.exists() ||
                        UnzipUseCase(
                            zipFilePath = result.absolutePath
                        ).unzip(
                            targetDirectoryPath = directory.absolutePath,
                            callback = null
                        )
                    ) {
                        unzippedFileCallback.onSuccess(directory.absolutePath)
                    } else {
                        resultCallback.onError("Can't unarchive editor")
                    }
                }
            },
            progressCallback = progressCallback
        )
    }
}