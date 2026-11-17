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
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
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

    companion object {
        private const val LOCK_REQUEST_CODE = 1003
        private const val FILE_CHOOSER_REQUEST_CODE = 1004
        private const val PERMISSIONS_REQUEST_CODE = 1005
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        try {
            when (requestCode) {
                LOCK_REQUEST_CODE -> {
                    when (resultCode) {
                        RESULT_OK -> AppLockState.isUnlockedThisSession = true
                        RESULT_FIRST_USER -> {
                            // "Switch account" - PIN was cleared, send them to a fresh login.
                            AppLockState.isUnlockedThisSession = true
                            webView.loadUrl("https://truedata.com.ng/mobile/login")
                        }
                        else -> {
                            // Lock screen was somehow dismissed without success - stay
                            // locked, try again next time the app resumes.
                        }
                    }
                }
                FILE_CHOOSER_REQUEST_CODE -> {
                    val results: Array<Uri>? = if (resultCode == RESULT_OK && data != null) {
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
            }
        } catch (e: Throwable) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            android.app.AlertDialog.Builder(this)
                .setTitle("A screen result crashed")
                .setMessage(sw.toString())
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ -> finishAffinity() }
                .show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        // Nothing to do either way - the site itself re-checks/re-requests
        // camera access if it's actually needed for a specific upload.
    }

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
            requestPermissions(needed.toTypedArray(), PERMISSIONS_REQUEST_CODE)
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
            textZoom = 100
            userAgentString = userAgentString + " TrueDataApp/1.1"
        }
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)

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
                val url = request.url.toString()
                val host = request.url.host ?: ""
                val scheme = (request.url.scheme ?: "").lowercase()
                if (scheme == "about" || scheme == "data" || scheme == "blob" || scheme == "javascript") {
                    return false
                }
                // Our own site stays inside the app.
                if ((url.startsWith("https://") || url.startsWith("http://")) &&
                    (host == "truedata.com.ng" || host.endsWith(".truedata.com.ng"))) {
                    return false
                }
                openOutside(url)
                return true
            }

            override fun onPageFinished(view: WebView, url: String) {
                swipeRefresh.isRefreshing = false
                CookieManager.getInstance().flush()
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
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST_CODE)
                    true
                } catch (_: Exception) {
                    filePathCallback = null
                    false
                }
            }
        }

        // Handles file downloads (e.g. transaction receipts) via the system Download Manager
        webView.setDownloadListener { url, _, contentDisposition, mimeType, _ ->
            try {
                if (!url.startsWith("http")) {
                    // blob:/data: files (e.g. receipt images) can't go to Download Manager.
                    android.widget.Toast.makeText(this, "Use the Share or Save Image button on the page", android.widget.Toast.LENGTH_SHORT).show()
                    return@setDownloadListener
                }
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimeType)
                request.addRequestHeader("Cookie", CookieManager.getInstance().getCookie(url) ?: "")
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                val fileName = URLUtilGuessFileName(url, contentDisposition, mimeType)
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, fileName)
                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)
                android.widget.Toast.makeText(this, "Downloading...", android.widget.Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
                android.widget.Toast.makeText(this, "Download failed", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun URLUtilGuessFileName(url: String, contentDisposition: String?, mimeType: String?): String {
        return android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
    }

    private fun setupSwipeRefresh() {
        // Pull-to-refresh is OFF: on pages with their own scrolling list (data
        // plans etc.) it fired while customers scrolled, reloading the page.
        swipeRefresh.isEnabled = false
    }

    /** Opens WhatsApp, phone, RawBT printer (intent:), Play Store or any other site outside the app. */
    private fun openOutside(url: String) {
        try {
            if (url.startsWith("intent:")) {
                val intent = Intent.parseUri(url, Intent.URI_INTENT_SCHEME)
                intent.addCategory(Intent.CATEGORY_BROWSABLE)
                intent.component = null
                try {
                    startActivity(intent)
                } catch (e: android.content.ActivityNotFoundException) {
                    val fallback = intent.getStringExtra("browser_fallback_url")
                    val pkg = intent.`package`
                    when {
                        fallback != null -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
                        pkg != null -> startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")))
                    }
                }
            } else {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        } catch (_: Throwable) {
            android.widget.Toast.makeText(this, "No app found to open this link", android.widget.Toast.LENGTH_SHORT).show()
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (offlineLayout.visibility == View.VISIBLE) {
            showExitDialog()
            return
        }
        val url = webView.url ?: ""
        val onHome = url.trimEnd('/').endsWith("/mobile/home") ||
                url.endsWith("/mobile/home/index.php") || url.trimEnd('/').endsWith("/mobile")
        if (!onHome && webView.canGoBack()) {
            webView.goBack()
        } else {
            showExitDialog()
        }
    }

    private fun showExitDialog() {
        android.app.AlertDialog.Builder(this)
            .setTitle("Leave TrueDataSUP?")
            .setItems(arrayOf("Exit app", "Minimize", "Cancel")) { dialog, which ->
                when (which) {
                    0 -> finishAffinity()
                    1 -> moveTaskToBack(true)
                    else -> dialog.dismiss()
                }
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        try {
            // Every time the app comes to the foreground (cold start, or coming
            // back from another app) - if a PIN has been saved on this device,
            // require it (or fingerprint) again before showing anything.
            if (pinStore.isPinSet() && !AppLockState.isUnlockedThisSession) {
                startActivityForResult(Intent(this, LockActivity::class.java), LOCK_REQUEST_CODE)
            }
        } catch (e: Throwable) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            android.app.AlertDialog.Builder(this)
                .setTitle("Lock screen launch crashed")
                .setMessage(sw.toString())
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ -> finishAffinity() }
                .show()
        }
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    override fun onStop() {
        super.onStop()
        // Leaving the app resets the unlock state, so returning later
        // requires the PIN/fingerprint again.
        AppLockState.isUnlockedThisSession = false
    }
}
