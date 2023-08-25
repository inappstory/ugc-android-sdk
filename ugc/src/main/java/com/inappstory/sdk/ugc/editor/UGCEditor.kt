package com.inappstory.sdk.ugc.editor

import android.app.Activity
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.webkit.*
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.inappstory.sdk.InAppStoryManager
import com.inappstory.sdk.network.JsonParser
import com.inappstory.sdk.network.jsapiclient.JsApiClient
import com.inappstory.sdk.share.IASShareManager
import com.inappstory.sdk.stories.api.models.WebResource
import com.inappstory.sdk.stories.api.models.logs.WebConsoleLog
import com.inappstory.sdk.stories.ui.views.IASWebView
import com.inappstory.sdk.stories.utils.Sizes
import com.inappstory.sdk.ugc.IUGCReaderLoaderView
import com.inappstory.sdk.ugc.R
import com.inappstory.sdk.ugc.UGCInAppStoryManager
import com.inappstory.sdk.ugc.picker.FileChooseActivity
import com.inappstory.sdk.utils.ZipLoadCallback
import com.inappstory.sdk.utils.ZipLoader
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.max


internal class UGCEditor : AppCompatActivity() {
    private lateinit var webView: IASWebView
    private lateinit var loader: ImageView
    private lateinit var closeButton: View
    private lateinit var webViewContainer: View
    private lateinit var loaderContainer: RelativeLayout
    private lateinit var loaderView: IUGCReaderLoaderView
    private lateinit var baseContainer: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        setContentView(R.layout.cs_activity_ugc)
        UGCInAppStoryManager.editorCallback.editorEvent("editorWillShow");
        setViews()
        initWebView()
        config = intent.getStringExtra("editorConfig")
        loadEditor(intent.getStringExtra("url"))
    }

    override fun onStart() {
        super.onStart()
        addBroadcastListener()
    }

    override fun onStop() {
        removeBroadcastListener()
        super.onStop()
    }

    private fun loadJsApiResponse(gameResponse: String, cb: String) {
        webView.evaluateJavascript("$cb('$gameResponse');", null)
    }

    private var editorState: String? = null

    internal companion object {
        const val EDITOR_STATE_KEY = "editorState"
        const val CLOSE_UGC_EDITOR_MSG = "closeUGCEditor"
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(EDITOR_STATE_KEY, editorState)
        super.onSaveInstanceState(outState)
        editorState = null
    }


    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        editorState = savedInstanceState.getString(EDITOR_STATE_KEY)
        super.onRestoreInstanceState(savedInstanceState)
    }

    private fun recreateEditor() {
        editorState?.let {
            val js = "window.editorApi.onRestoreInstanceState($it);"
            if (this::webView.isInitialized) {
                webView.evaluateJavascript(
                    js,
                    null
                )
                webView.alpha = 0f
            }
        }
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
    private var ugcLoaded = false
    private var handleBack = false
    var openFilePickerCbName: String? = null
    var openFilePickerCbId: String? = null

    fun closeEditor() {
        finish()
    }

    fun editorLoaded(data: String) {
        val editorLoadedResult = JsonParser.fromJson(
            data,
            EditorLoadedResult::class.java
        )
        handleBack = editorLoadedResult.backHandler ?: false
        ugcLoaded = true
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
        webView.settings.allowFileAccessFromFileURLs = true
        webView.settings.allowUniversalAccessFromFileURLs = true
        loader = findViewById(R.id.loader)
        baseContainer = findViewById(R.id.draggable_frame)
        loaderContainer = findViewById(R.id.loaderContainer)
        loaderView = UGCInAppStoryManager.loaderView ?: UGCLoadProgressBar(this@UGCEditor)
        loaderView.setIndeterminate(false)
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
        loaderContainer.addView(loaderView.getView(this))
    }

    private fun saveEditorState() {
        if (this::webView.isInitialized) {
            webView.evaluateJavascript(
                "window.editorApi.onSaveInstanceState();"
            ) { s ->
                editorState = s
            }
        }
    }

    private fun pauseEditor() {
        if (this::webView.isInitialized) {
            webView.evaluateJavascript("window.editorApi.pauseUI();", null)
            webView.alpha = 0f
        }
    }

    private fun resumeEditor() {
        if (this::webView.isInitialized) {
            webView.alpha = 1f
            webView.evaluateJavascript("window.editorApi.resumeUI();", null)
        }
    }

    override fun onPause() {
        super.onPause()
        saveEditorState()
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
        recreateEditor()
        resumeEditor()
    }

    lateinit var broadcastReceiver: BroadcastReceiver

    private fun removeBroadcastListener() {
        if (this::broadcastReceiver.isInitialized) {
            unregisterReceiver(broadcastReceiver);
        }
    }


    private fun addBroadcastListener() {
        broadcastReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent == null) return
                when (intent.action) {
                    CLOSE_UGC_EDITOR_MSG -> close()
                }
            }

        }
        val filter = IntentFilter(CLOSE_UGC_EDITOR_MSG)
        registerReceiver(broadcastReceiver, filter)
    }

    private fun filterTypes(types: List<String>?): Pair<String?, ArrayList<String>> {
        val res = arrayListOf<String>()
        var isVideo = false
        var isImage = false
        var resType: String? = null
        types?.forEach {
            if (it.startsWith(imageType)) {
                res.add(it)
                resType = imageType

            }
            if (it.startsWith(videoType)) {
                res.add(it)
                resType = videoType
            }
        }
        return Pair(resType, res)
    }


    private val videoType = "video"
    private val imageType = "image"

    private fun sendWebConsoleLog(
        consoleMessage: ConsoleMessage,
    ) {
        val log = WebConsoleLog()
        log.timestamp = System.currentTimeMillis()
        log.id = UUID.randomUUID().toString()
        log.logType = consoleMessage.messageLevel().name
        log.message = consoleMessage.message()
        log.sourceId = consoleMessage.sourceId()
        log.lineNumber = consoleMessage.lineNumber()
        log.storyId = "UGC_Editor"
        log.slideIndex = -1
        InAppStoryManager.sendWebConsoleLog(log)
    }

    private fun initWebView() {
        webView.settings.minimumFontSize = 1
        webView.addJavascriptInterface(
            UGCJSInterface(
                this@UGCEditor
            ), "Android"
        )

        webView.webChromeClient = object : WebChromeClient() {
            var init = false

            override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                sendWebConsoleLog(
                    consoleMessage
                )
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

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // `http://file-assets` - special protocol and Uri first part
                // Bcz WebView can`t fetch with `file` protocol
                return if (request.url.toString().startsWith("http://file-assets")) {

                    // convert to normal Uri and get decoded path (decode %20 to space and etc)
                    val filePath = Uri.parse(
                        request.url.toString().replace("http://file-assets", "file://")
                    ).path
                    if (filePath != null) {

                        val file = File(filePath)
                        if (file.exists()) {

                            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                MimeTypeMap.getFileExtensionFromUrl(filePath)
                            )

                            WebResourceResponse(mimeType, "utf-8", FileInputStream(file))/*.apply {
                                val headers = HashMap(responseHeaders ?: emptyMap())
                                headers["Access-Control-Allow-Origin"] = "*"
                                responseHeaders = headers
                            }*/

                        } else {
                            Log.d("InAppStory_UGC", "File ${filePath} not exists")
                            super.shouldInterceptRequest(view, request)
                        }
                    } else {
                        Log.d("InAppStory_UGC", "Empty filePath for Uri ${request.url}")
                        super.shouldInterceptRequest(view, request)
                    }

                } else {
                    super.shouldInterceptRequest(view, request)
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
        UGCInAppStoryManager.editorCallback.editorEvent("editorDidClose");
        super.onDestroy()
        UGCInAppStoryManager.invokeCloseCallback()
    }

    var callback: ZipLoadCallback = object : ZipLoadCallback {
        override fun onLoad(baseUrl: String?, data: String?) {
            webView.loadDataWithBaseURL(
                baseUrl, data!!,
                "text/html; charset=utf-8", "UTF-8",
                null
            )
        }

        override fun onError(error: String) {
            TODO("Not yet implemented")
        }

        override fun onProgress(loadedSize: Long, totalSize: Long) {
            loaderView.setProgress((loadedSize * 100 / totalSize).toInt(), 100)
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
                var arr = arrayOf<String>()
                if (resultCode == Activity.RESULT_OK) {
                    val files = data?.getStringArrayExtra("files")
                   /* files?.let {
                        if (it.isNotEmpty())
                            if (it[0].endsWith("mp4"))
                                testSend(File(it[0]))
                    }*/
                    arr = files?.map {
                        Uri.fromFile(File(it)).toString()
                            .replace("file://", "http://file-assets")
                    }?.toTypedArray() ?: emptyArray()
                }
                val responseMap =
                    mapOf("id" to openFilePickerCbId, "response" to arr)
                val payload = JsonParser.mapToJsonString(responseMap).replace("'".toRegex(), "\\'")
                val initST = "window.editorApi.${openFilePickerCbName}('${payload}');"
                webView.evaluateJavascript(initST, null)
                openFilePickerCbId = null
            }
        }
    }

    private fun testSend(file: File) {
        lifecycleScope.launch {
            delay(15000)
            val sendingIntent = Intent()
            sendingIntent.action = Intent.ACTION_SEND
            sendingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sendingIntent.type = "video/*"
            sendingIntent.putExtra(
                Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                    this@UGCEditor,
                    "com.inappstory.sdk.ugc.provider", //(use your app signature + ".provider" )
                    file
                )
            )
            startActivity(Intent.createChooser(sendingIntent, null))
        }
    }

    override fun onBackPressed() {
        if (handleBack) {
            gestureBack()
        } else {
            close()
        }
    }


    private fun close() {
        if (ugcLoaded) {
            webView.evaluateJavascript(
                "window.editorApi.close();", null
            )
            clearConvertedVideos()
        } else {
            finish()
        }
    }

    private fun clearConvertedVideos() {
        val dir = File("$filesDir/converted")
        if (dir.exists() && dir.isDirectory) {
            dir.listFiles()?.forEach { it.deleteRecursively() }
        }
    }

    fun openFilePicker(
        data: String
    ): Void? {
        val config = JsonParser.fromJson(
            data,
            EditorOpenFilePickerConfig::class.java
        )
        openFilePickerCbName = config.cb
        openFilePickerCbId = config.id


        val acceptTypes = config.accept.split(",")
        var hasVideo = false
        var hasPhoto = false
        acceptTypes.forEach {
            if (it.startsWith("image")) hasPhoto = true
            if (it.startsWith("video")) hasVideo = true
        }
        if (!(hasVideo || hasPhoto)) return null;

        val newIntent = Intent(
            this@UGCEditor,
            FileChooseActivity::class.java
        )

        newIntent.putExtra(
            "contentType", when {
                hasVideo && hasPhoto -> 0 //Mix
                hasVideo -> 2 //Video
                else -> 1 //Photo
            }
        )

        val messageNames = intent.getStringArrayExtra("messageNames")
        val messages = intent.getStringArrayExtra("messages")
        val filePickerFilesLimit = intent.getIntExtra("filePickerFilesLimit", 10)
        val filePickerPhotoSizeLimit =
            intent.getLongExtra("filePickerImageMaxSizeInBytes", 30000000L)
        val filePickerVideoSizeLimit =
            intent.getLongExtra("filePickerVideoMaxSizeInBytes", 30000000L)
        val filePickerFileDurationLimit =
            intent.getLongExtra("filePickerVideoMaxLengthInSeconds", 30)

        newIntent.putStringArrayListExtra(
            "acceptTypes",
            ArrayList(acceptTypes)
        )

        newIntent.putExtra("messageNames", messageNames)
        newIntent.putExtra("messages", messages)
        newIntent.putExtra(
            "allowMultiple",
            config.multiple == true
        )
        newIntent.putExtra("filePickerFilesLimit", filePickerFilesLimit)
        newIntent.putExtra("filePickerImageMaxSizeInBytes", filePickerPhotoSizeLimit)
        newIntent.putExtra("filePickerVideoMaxSizeInBytes", filePickerVideoSizeLimit)
        newIntent.putExtra("filePickerVideoMaxLengthInSeconds", filePickerFileDurationLimit)
        startActivityForResult(newIntent, CHOOSE_FILE_REQUEST_CODE)
        return null;
    }

}


