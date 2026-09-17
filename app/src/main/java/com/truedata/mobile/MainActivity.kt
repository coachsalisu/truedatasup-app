package com.truedata.mobile

import android.Manifest
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var offlineLayout: LinearLayout

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private val targetUrl by lazy {
        val path = intent.getStringExtra("TARGET_PATH") ?: ""
        "https://truedata.com.ng/mobile/" + path.removePrefix("/")
    }
    private val offlineCache by lazy { OfflineCache(this) }
    private val pinStore by lazy { PinStore(this) }

    // Shows the lock screen and reacts to how the person got past it (PIN/
    // fingerprint success, or "Not you? Log in differently").
    private val lockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
                RESULT_OK -> AppLockState.isUnlockedThisSession = true
                RESULT_FIRST_USER -> {
                    // "Switch account" - PIN was cleared, send them to a fresh login.
                    AppLockState.isUnlockedThisSession = true
                    webView.loadUrl("https://truedata.com.ng/mobile/login")
                }
                else -> {
                    // Lock screen was somehow dismissed without success - stay locked,
                    // try again next time the app resumes.
                }
            }
        }

    // Handles the result of the file picker (for KYC photo/document uploads on your site)
    private val fileChooserLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            val data = result.data
            val results: Array<Uri>? = if (result.resultCode == RESULT_OK && data != null) {
                if (data.clipData != null) {
                    val count = data.clipData!!.itemCount
                    Array(count) { i -> data.clipData!!.getItemAt(i).uri }
                } else {
                    data.data?.let { arrayOf(it) }
                }
            } else null
            filePathCallback?.onReceiveValue(results)
            filePathCallback = null
        }

    // Requests camera + notification permission (Android 13+) up front
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_main)

            webView = findViewById(R.id.webView)
            swipeRefresh = findViewById(R.id.swipeRefresh)
            offlineLayout = findViewById(R.id.offlineLayout)

            requestRuntimePermissions()
            setupWebView()
            setupSwipeRefresh()
            setupOfflineRetry()

            // Always attempt to load - if we're offline, the OfflineCache (wired
            // into shouldInterceptRequest above) will serve a saved copy of the
            // dashboard if one exists from a previous visit. onReceivedError only
            // fires, and shows the full offline screen, when nothing is cached
            // yet for this page (e.g. first-ever launch with no internet).
            webView.loadUrl(targetUrl)
        } catch (e: Throwable) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            android.app.AlertDialog.Builder(this)
                .setTitle("Main screen crashed")
                .setMessage(sw.toString())
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ -> finishAffinity() }
                .show()
        }
    }

    private fun requestRuntimePermissions() {
        val perms = mutableListOf(Manifest.permission.CAMERA)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val needed = perms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    @Suppress("SetJavaScriptEnabled")
    private fun setupWebView() {
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            useWideViewPort = true
            loadWithOverviewMode = true
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            mediaPlaybackRequiresUserGesture = false
        }

        // Lets the website hand a just-confirmed PIN to the app for secure
        // local storage (as a salted hash - see PinStore). The site only
        // calls this when it detects it's running inside our app.
        webView.addJavascriptInterface(WebAppInterface(this, pinStore), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                // Route every request through the offline cache: it tries the
                // live network first and only falls back to a saved copy if
                // that fails. Returning null here means "handle normally".
                return try {
                    offlineCache.intercept(request)
                } catch (e: Throwable) {
                    // Never let a problem here crash the app - this runs on
                    // every single request the page makes, so it must fail
                    // safe. Falling back to null just means "load normally".
                    null
                }
            }

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Keep every truedata.com.ng link inside the app; only send truly external
                // links (e.g. WhatsApp, a payment gateway) out to their own app/browser.
                val url = request.url.toString()
                return if (url.contains("truedata.com.ng")) {
                    false
                } else {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, request.url))
                    } catch (_: Exception) { }
                    true
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                swipeRefresh.isRefreshing = false

                // The website's own JS sends people here after a successful
                // login or registration+PIN setup. The moment we land here,
                // switch to the native dashboard instead of showing this page.
                val isRawDashboard = url == "https://truedata.com.ng/mobile/home/" ||
                        url == "https://truedata.com.ng/mobile/home/index.php" ||
                        url == "https://truedata.com.ng/mobile/"
                if (isRawDashboard) {
                    try {
                        startActivity(Intent(this@MainActivity, DashboardActivity::class.java))
                        finish()
                    } catch (e: Throwable) {
                        val sw = java.io.StringWriter()
                        e.printStackTrace(java.io.PrintWriter(sw))
                        android.app.AlertDialog.Builder(this@MainActivity)
                            .setTitle("Dashboard switch crashed")
                            .setMessage(sw.toString())
                            .setCancelable(false)
                            .setPositiveButton("Close") { _, _ -> finishAffinity() }
                            .show()
                    }
                }
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    swipeRefresh.isRefreshing = false
                    showOffline()
                }
            }

            override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
                // Do not silently accept bad SSL certs - keep this cancel() as-is for security.
                handler.cancel()
            }
        }

        // Handles <input type="file"> for document/photo uploads on your site (e.g. KYC)
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView,
                callback: ValueCallback<Array<Uri>>,
                params: FileChooserParams
            ): Boolean {
                filePathCallback = callback
                val intent = params.createIntent()
                return try {
                    fileChooserLauncher.launch(intent)
                    true
                } catch (_: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }

        // Handles file downloads (e.g. transaction receipts) via the system Download Manager
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            val fileName = URLUtilGuessFileName(url, contentDisposition, mimeType)
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
    }

    private fun URLUtilGuessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.brand_purple)
        swipeRefresh.setOnRefreshListener {
            if (isOnline()) {
                webView.reload()
            } else {
                swipeRefresh.isRefreshing = false
                android.widget.Toast.makeText(this, "Still offline - showing saved data", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupOfflineRetry() {
        val retryButton = findViewById<Button>(R.id.retryButton)
        retryButton.setOnClickListener {
            if (isOnline()) {
                hideOffline()
                webView.loadUrl(targetUrl)
            } else {
                android.widget.Toast.makeText(this, "Still no internet connection", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showOffline() {
        offlineLayout.visibility = View.VISIBLE
    }

    private fun hideOffline() {
        offlineLayout.visibility = View.GONE
    }

    private fun isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        // Every time the app comes to the foreground (cold start, or coming
        // back from another app) - if a PIN has been saved on this device,
        // require it (or fingerprint) again before showing anything.
        if (pinStore.isPinSet() && !AppLockState.isUnlockedThisSession) {
            lockLauncher.launch(Intent(this, LockActivity::class.java))
        }
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app resets the unlock state, so returning later
        // requires the PIN/fingerprint again.
        AppLockState.isUnlockedThisSession = false
    }
}
