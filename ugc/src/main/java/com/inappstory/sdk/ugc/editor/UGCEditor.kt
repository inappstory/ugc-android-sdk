package com.inappstory.sdk.ugc.editor

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.ConsoleMessage
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import com.inappstory.sdk.AppearanceManager
import com.inappstory.sdk.game.reader.GameLoadProgressBar
import com.inappstory.sdk.network.JsonParser
import com.inappstory.sdk.network.jsapiclient.JsApiClient
import com.inappstory.sdk.stories.api.models.WebResource
import com.inappstory.sdk.stories.ui.views.IASWebView
import com.inappstory.sdk.stories.ui.views.IGameLoaderView
import com.inappstory.sdk.stories.utils.Sizes
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.UGCInAppStoryManager
import com.inappstory.sdk.utils.ZipLoadCallback
import com.inappstory.sdk.utils.ZipLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.ArrayList
import kotlin.math.max

internal class UGCEditor : AppCompatActivity() {
    private lateinit var webView: IASWebView
    private lateinit var loader: ImageView
    private lateinit var closeButton: View
    private lateinit var webViewContainer: View
    private lateinit var loaderContainer: RelativeLayout
    private lateinit var loaderView: IGameLoaderView
    private lateinit var baseContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        UGCInAppStoryManager.currentEditor = this
        setContentView(R.layout.cs_activity_ugc)
        UGCInAppStoryManager.editorCallback.editorEvent("editorWillShow");
        setViews()
        initWebView()
        config = intent.getStringExtra("editorConfig")
        //     JsonParser.fromJson(, EditorConfig::class.java)
        loadEditor(intent.getStringExtra("url"))
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
    }


    override fun onRestart() {
        super.onRestart()
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        Log.e("UGC_Lifecycle", "onRestoreInstanceState $savedInstanceState")
    }

    fun loadJsApiResponse(gameResponse: String, cb: String) {
        webView.evaluateJavascript("$cb('$gameResponse');", null)
    }

    fun sendApiRequest(data: String?) {
        JsApiClient(this).sendApiRequest(
            data
        ) { result, cb -> loadJsApiResponse(modifyJsResult(result), cb) }
    }


    fun sendEditorEvent(event: String, payload: String?) {
        UGCInAppStoryManager.editorCallback.editorEvent(event, HashMap(JsonParser.toMap(payload)))
    }

    var config: String? = null
    var ugcLoaded = false
    var handleBack = false

    fun updateUI() {
        CoroutineScope(Dispatchers.Main).launch {
            closeButton.visibility = View.GONE
            loaderContainer.visibility = View.GONE
        }
    }

    private fun modifyJsResult(data: String?): String {
        if (data == null) return ""
        data.replace("'".toRegex(), "\\'")
        return data
    }

    private fun setViews() {
        webView = findViewById(R.id.ugcWebview)
        loader = findViewById(R.id.loader)
        baseContainer = findViewById(R.id.draggable_frame)
        loaderContainer = findViewById(R.id.loaderContainer)
        loaderView = if (AppearanceManager.getCommonInstance().csGameLoaderView() == null) {
            GameLoadProgressBar(
                this@UGCEditor,
                null,
                android.R.attr.progressBarStyleHorizontal
            )
        } else {
            AppearanceManager.getCommonInstance().csGameLoaderView()
        }
        if (Sizes.isTablet()) {
            baseContainer.setOnClickListener { close() }
        }
        closeButton = findViewById(R.id.close_button)
        closeButton.setOnClickListener { close() }
        webViewContainer = findViewById(R.id.webViewContainer)
        if (!Sizes.isTablet()) {
            if (Build.VERSION.SDK_INT >= 28) {
                Handler(mainLooper).post {
                    if (window != null && window.decorView.rootWindowInsets != null) {
                        val cutout = window.decorView.rootWindowInsets.displayCutout
                        if (cutout != null) {
                            val lp1 =
                                webViewContainer.layoutParams as LinearLayout.LayoutParams
                            lp1.topMargin = max(cutout.safeInsetTop, 0)
                            webViewContainer.layoutParams = lp1
                        }
                    }
                }
            }
        }
        loaderContainer.addView(loaderView.view)
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    private fun pauseEditor() {
        webView.evaluateJavascript("window.editorApi.pauseUI();", null)
    }

    private fun resumeEditor() {
        webView.evaluateJavascript("window.editorApi.resumeUI();", null)
    }

    override fun onPause() {
        super.onPause()
        pauseEditor()
    }


    private fun gestureBack() {
        if (ugcLoaded) {
            webView.evaluateJavascript(
                "window.editorApi.handleBack();"
            ) { s -> if (s != "true") close() }
        } else {
            close()
        }
    }

    override fun onResume() {
        super.onResume()
        resumeEditor()
    }

    private fun filterTypes(types: List<String>?): Pair<String?, ArrayList<String>> {
        val res = arrayListOf<String>()
        var isVideo = false
        var isImage = false
        var resType: String? = null
        types?.forEach {
            if (it.startsWith(imageType)) {
                if (!isVideo) {
                    isImage = true
                    res.add(it)
                    resType = imageType
                }
            }
            if (it.startsWith(videoType)) {
                if (!isImage) {
                    isVideo = true
                    res.add(it)
                    resType = videoType
                }
            }
        }
        return Pair(resType, res)
    }


    private val videoType = "video"
    private val imageType = "image"

    private fun initWebView() {
        webView.settings.minimumFontSize = 1
        webView.addJavascriptInterface(
            UGCJSInterface(
                this@UGCEditor
            ), "Android"
        )
        webView.webChromeClient = object : WebChromeClient() {
            var init = false
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {

                this@UGCEditor.filePathCallback = filePathCallback
                val filteredTypes = filterTypes(fileChooserParams?.acceptTypes?.asList())
                if (filteredTypes.first.orEmpty().isNotEmpty()) {
                    val intent = Intent(
                        this@UGCEditor,
                        FileChooseActivity::class.java
                    )
                    intent.putStringArrayListExtra(
                        "acceptTypes",
                        filteredTypes.second
                    )
                    intent.putExtra("type", filteredTypes.first)
                    startActivityForResult(intent, CHOOSE_FILE_REQUEST_CODE)
                }
                return true
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                Log.d(
                    "InAppStory_UGC", consoleMessage.messageLevel().name + ": "
                            + consoleMessage.message() + " -- From line "
                            + consoleMessage.lineNumber() + " of "
                            + consoleMessage.sourceId()
                )
                return super.onConsoleMessage(consoleMessage)
            }


            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                if (newProgress > 10) {
                    if (!init && config != null) {
                        init = true
                        initEditor(config)
                    }
                }
            }
        }

    }

    private fun initEditor(data: String?) {
        if (data == null) return
        val initST = "window.editor = (function() {var self = window.editor || {};" +
                " self._e = self._e || []; self.ready = self.ready || function (f) " +
                "{self._e.push(f);}; return self; }()); window.editor.ready(function ()" +
                " { window.editorApi && window.editorApi.init(${data}); });"
        webView.evaluateJavascript(initST, null)
    }

    override fun onDestroy() {
        if (UGCInAppStoryManager.currentEditor === this)
            UGCInAppStoryManager.currentEditor = null
        UGCInAppStoryManager.editorCallback.editorEvent("editorDidClose");
        super.onDestroy()
        closeCallback?.invoke()
    }

    var callback: ZipLoadCallback = object : ZipLoadCallback {
        override fun onLoad(baseUrl: String?, data: String?) {
            webView.loadDataWithBaseURL(
                baseUrl, data!!,
                "text/html; charset=utf-8", "UTF-8",
                null
            )
        }

        override fun onError() {
            TODO("Not yet implemented")
        }

        override fun onProgress(loadedSize: Int, totalSize: Int) {
            loaderView.setProgress((loadedSize * 100 / totalSize), 100)
        }
    }

    fun loadEditor(path: String?) {
        val resourceList = ArrayList<WebResource>()
        val urlParts: Array<String> = ZipLoader.urlParts(path)
        ZipLoader.getInstance().downloadAndUnzip(resourceList, path, urlParts[0], callback, "ugc")
    }

    internal val CHOOSE_FILE_REQUEST_CODE = 827;

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            CHOOSE_FILE_REQUEST_CODE -> {
                var arr: Array<Uri> = arrayOf()
                if (resultCode == Activity.RESULT_OK) {
                    val filePath = data?.getStringExtra("file")
                    if (filePath != null) {
                        arr = arrayOf(Uri.fromFile(File(filePath)))
                    }
                }
                filePathCallback?.onReceiveValue(
                    arr
                )
                filePathCallback = null
            }
        }
    }

    override fun onBackPressed() {
        if (handleBack) {
            gestureBack()
        } else {
            close()
        }
    }

    private var closeCallback: (() -> Unit) = { }

    fun close(closeCallback: (() -> Unit) = {}) {
        this.closeCallback = closeCallback
        if (ugcLoaded) {
            webView.evaluateJavascript(
                "window.editorApi.close();", null
            )
        } else {
            finish()
        }
    }
}


