package com.inappstory.sdk.ugc

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.provider.Settings
import com.inappstory.sdk.AppearanceManager
import com.inappstory.sdk.InAppStoryManager
import com.inappstory.sdk.network.JsonParser
import com.inappstory.sdk.stories.api.models.StatisticSession
import com.inappstory.sdk.stories.api.models.callbacks.OpenSessionCallback
import com.inappstory.sdk.stories.ui.ScreensManager
import com.inappstory.sdk.stories.ui.reader.StoriesActivity
import com.inappstory.sdk.stories.ui.reader.StoriesFixedActivity
import com.inappstory.sdk.stories.utils.SessionManager
import com.inappstory.sdk.ugc.editor.EditorConfig
import com.inappstory.sdk.ugc.editor.UGCEditor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*

object UGCInAppStoryManager {
    internal var currentEditor: UGCEditor? = null

    fun openEditor(
        context: Context,
        ugcInitData: UGCInitData? = null

    ) {
        if (InAppStoryManager.getInstance() == null) return
        SessionManager.getInstance().useOrOpenSession(object : OpenSessionCallback {
            override fun onSuccess() {
                GlobalScope.launch(Dispatchers.Main) {
                    ScreensManager.getInstance().ugcCloseCallback =
                        ScreensManager.CloseUgcReaderCallback {
                            if (currentEditor != null) {
                                currentEditor?.close()
                                currentEditor = null
                            }
                        }

                    ugcInitData?.let {

                    }
                    val configSt = JsonParser.getJson(genEditorConfig(context))
                    val intent = Intent(
                        context,
                        UGCEditor::class.java
                    )
                    intent.putExtra("editorConfig", configSt)
                    intent.putExtra("url", StatisticSession.getInstance()?.editor?.url ?: "")
                    context.startActivity(intent)
                }
            }

            override fun onError() {

            }
        })

    }

    private fun genEditorConfig(context: Context): EditorConfig {
        return EditorConfig().apply {
            userId = InAppStoryManager.getInstance()?.userId
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            )
            lang = (Locale.getDefault().toLanguageTag()).lowercase()
            appPackageId = context.packageName
            sdkVersion = InAppStoryManager.getLibraryVersion().first
            sessionId = StatisticSession.getInstance()?.id ?: ""
            config = StatisticSession.getInstance()?.editor?.config
            apiKey = InAppStoryManager.getInstance().apiKey
                ?: context.resources.getString(R.string.csApiKey)
        }
    }
}