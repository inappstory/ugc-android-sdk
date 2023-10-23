package com.inappstory.sdk.ugc

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Pair
import com.inappstory.sdk.InAppStoryManager
import com.inappstory.sdk.stories.ui.ScreensManager
import com.inappstory.sdk.ugc.cache.EditorCacheManager
import com.inappstory.sdk.ugc.editor.EmptyUGCEditorCallback
import com.inappstory.sdk.ugc.editor.UGCEditor
import com.inappstory.sdk.ugc.extinterfaces.IOpenSessionCallback
import com.inappstory.sdk.ugc.usecases.AddOpenSessionCallback
import kotlinx.coroutines.*
import java.util.*


object UGCInAppStoryManager {

    var editorCallback: UGCEditorCallback = EmptyUGCEditorCallback()
    private lateinit var appContext: Context

    var editorCacheManager: EditorCacheManager = EditorCacheManager()

    private var latestCloseCallback: (() -> Unit) = {}

    var loaderView: IUGCReaderLoaderView? = null

    fun init(context: Context) {
        appContext = context.applicationContext
        AddOpenSessionCallback().add(object : IOpenSessionCallback {
            override fun onSuccess() {
                editorCacheManager.clear()
            }

            override fun onError() {

            }
        })
    }

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
        if (InAppStoryManager.getInstance() == null) return
        if (!this::appContext.isInitialized) {
            return
        }
        openUgcEditorScreen(context, ugcInitData)
    }

    private fun openUgcEditorScreen(context: Context, ugcInitData: HashMap<String, Any?>? = null) {
        ScreensManager.getInstance().ugcCloseCallback =
            ScreensManager.CloseUgcReaderCallback {
                CoroutineScope(Dispatchers.Main).launch {
                    closeUGCEditor()
                }
            }
        CoroutineScope(Dispatchers.Main).launch {
            val intent = Intent(
                context,
                UGCEditor::class.java
            )
            if (context is Application) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            }
            intent.putExtra("ugcInitData", ugcInitData)
            context.startActivity(
                intent
            )
        }

    }

    fun getLibraryVersion(): Pair<String?, Int?> {
        return Pair(BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE)
    }
}