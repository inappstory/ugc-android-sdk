package com.inappstory.sdk.ugc.cache

import com.inappstory.sdk.stories.cache.DownloadInterruption
import com.inappstory.sdk.ugc.BuildConfig
import com.inappstory.sdk.ugc.extinterfaces.IGetUgcEditorCallback
import com.inappstory.sdk.ugc.extinterfaces.IUgcEditor
import com.inappstory.sdk.ugc.usecases.GetUgcEditor
import java.io.File

class EditorCacheManager {

    private var ugcEditor: IUgcEditor? = null

    fun clear() {
        ugcEditor = null
    }
    private fun getEditorModel(resultCallback: UseCaseCallback<IUgcEditor>) {
        val localEditor = ugcEditor
        if (localEditor == null) {
            GetUgcEditor().get(object : IGetUgcEditorCallback {
                override fun get(iEditor: IUgcEditor?) {
                    iEditor?.let {
                        resultCallback.onSuccess(it)
                        return
                    }
                    resultCallback.onError(null)

                }

                override fun onError() {
                    resultCallback.onError(null)
                }
            })
        } else {
            resultCallback.onSuccess(localEditor)
        }
    }
    suspend fun getEditor(
        resultCallback: UseCaseCallback<FilePathAndContent>,
        modelCallback: UseCaseCallback<IUgcEditor>,
        progressCallback: ProgressCallback,
        interruption: DownloadInterruption
    ) {
        getEditorModel(resultCallback = object : UseCaseCallback<IUgcEditor> {
            override fun onError(message: String?) {
                resultCallback.onError(message)
            }

            override fun onSuccess(result: IUgcEditor) {
                modelCallback.onSuccess(result)

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
                val editorUrl = getActualUGCUrl(result)
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
        })
    }

    private fun getActualUGCUrl(ugcEditor: IUgcEditor): String {
        if (ugcEditor.versionsMap().isNullOrEmpty() ||
            ugcEditor.versionTemplate().isNullOrEmpty() ||
            ugcEditor.urlTemplate().isNullOrEmpty()
        ) return ugcEditor.url() ?: ""
        ugcEditor.versionsMap().sortedByDescending { it.minBuild() }.forEach {
            it.editor()?.let { ver ->
                if (BuildConfig.VERSION_CODE >= it.minBuild())
                    return ugcEditor.urlTemplate().replace(ugcEditor.versionTemplate(), ver)
            }
        }
        return ugcEditor.url() ?: ""
    }
}