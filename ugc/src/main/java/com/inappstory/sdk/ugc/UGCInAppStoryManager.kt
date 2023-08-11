package com.inappstory.sdk.ugc

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.core.util.Pair
import com.inappstory.sdk.InAppStoryManager
import com.inappstory.sdk.network.JsonParser
import com.inappstory.sdk.stories.api.models.Session
import com.inappstory.sdk.stories.api.models.callbacks.OpenSessionCallback
import com.inappstory.sdk.stories.ui.ScreensManager
import com.inappstory.sdk.stories.utils.SessionManager
import com.inappstory.sdk.ugc.editor.EditorConfig
import com.inappstory.sdk.ugc.editor.EmptyUGCEditorCallback
import com.inappstory.sdk.ugc.editor.UGCEditor
import kotlinx.coroutines.*
import java.util.*


object UGCInAppStoryManager {

    var editorCallback: UGCEditorCallback = EmptyUGCEditorCallback()
    lateinit var appContext: Context

    private var latestCloseCallback: (() -> Unit) = {}

    internal var loaderView: IUGCReaderLoaderView? = null

    fun csUgcReaderLoaderView(loaderView: IUGCReaderLoaderView) {
        this.loaderView = loaderView
    }

    fun closeEditor(closeCallback: (() -> Unit) = {}) {
        latestCloseCallback = closeCallback
        CoroutineScope(Dispatchers.Main).launch {
            closeUGCEditor()
        }
    }


    private fun closeUGCEditor() {
        val intent = Intent(UGCEditor.CLOSE_UGC_EDITOR_MSG)
        intent.setPackage(appContext.packageName)
        appContext.sendBroadcast(intent)
    }

    fun invokeCloseCallback() {
        latestCloseCallback.invoke()
        latestCloseCallback = {}
    }

    fun openEditor(
        context: Context,
        ugcInitData: HashMap<String, Any?>? = null
    ) {
        appContext = context.applicationContext
        if (InAppStoryManager.getInstance() == null) return
        SessionManager.getInstance().useOrOpenSession(object : OpenSessionCallback {
            override fun onSuccess() {
                ScreensManager.getInstance().ugcCloseCallback =
                    ScreensManager.CloseUgcReaderCallback {
                        CoroutineScope(Dispatchers.Main).launch {
                            closeUGCEditor()
                        }
                    }
                val config = genEditorConfig(context, ugcInitData)
                val configSt = JsonParser.getJson(config)
                val messages = Session.getInstance()?.editor?.messages
                val galleryFileMaxCount =
                    config.config?.getOrElse("filePickerFilesLimit") { 10 } as Int
                val filePickerPhotoSizeLimit =
                    config.config?.getOrElse("filePickerImageMaxSizeInBytes") { 30000000L } as Long
                val filePickerVideoSizeLimit =
                    config.config?.getOrElse("filePickerVideoMaxSizeInBytes") { 30000000L } as Long
                val filePickerFileDurationLimit =
                    config.config?.getOrElse("filePickerVideoMaxLengthInSeconds") { 30L } as Long
                CoroutineScope(Dispatchers.Main).launch {
                    val intent = Intent(
                        context,
                        UGCEditor::class.java
                    )
                    messages?.let {
                        val (keys, values) = messages.toList().unzip()
                        intent.putExtra("messageNames", keys.toTypedArray())
                        intent.putExtra("messages", values.toTypedArray())
                    }
                    intent.putExtra("filePickerFilesLimit", galleryFileMaxCount)
                    intent.putExtra("filePickerImageMaxSizeInBytes", filePickerPhotoSizeLimit)
                    intent.putExtra("filePickerVideoMaxSizeInBytes", filePickerVideoSizeLimit)
                    intent.putExtra(
                        "filePickerVideoMaxLengthInSeconds",
                        filePickerFileDurationLimit
                    )
                    intent.putExtra("editorConfig", configSt)
                    intent.putExtra("url", getActualUGCUrl())
                    context.startActivity(intent)
                }
            }

            override fun onError() {

            }
        })
    }

    private fun getActualUGCUrl(): String {
        Session.getInstance()?.apply {
            if (editor == null) return ""
            if (editor.versionsMap.isNullOrEmpty() ||
                editor.versionTemplate.isNullOrEmpty() ||
                editor.urlTemplate.isNullOrEmpty()
            ) return editor.url ?: ""
            var actualVer: String? = null
            editor.versionsMap.sortedByDescending { it.minBuild }.forEach {
                actualVer = it.editor
                if (BuildConfig.VERSION_CODE >= it.minBuild) return@forEach
            }
            actualVer?.let {
                return editor.urlTemplate.replace(editor.versionTemplate, it)
            }
            return editor.url ?: ""
        }
        return ""
    }

    fun getLibraryVersion(): Pair<String?, Int?> {
        return Pair(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }


    private fun genEditorConfig(
        context: Context,
        ugcInitData: HashMap<String, Any?>? = null
    ): EditorConfig {
        return EditorConfig().apply {
            userId = InAppStoryManager.getInstance()?.userId
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            lang = (Locale.getDefault().toLanguageTag()).lowercase()
            appPackageId = context.packageName
            sdkVersion = InAppStoryManager.getLibraryVersion().first
            ugcSdkVersion = getLibraryVersion().first
            sessionId = Session.getInstance()?.id ?: ""
            config = Session.getInstance()?.editor?.config
            apiKey = InAppStoryManager.getInstance().apiKey
                ?: context.resources.getString(R.string.csApiKey)
            storyPayload = ugcInitData
        }
    }
}