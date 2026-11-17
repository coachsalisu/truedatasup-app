package com.truedata.mobile

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

class OfflineCache(@Suppress("UNUSED_PARAMETER") context: Context) {
    fun intercept(@Suppress("UNUSED_PARAMETER") request: WebResourceRequest): WebResourceResponse? = null
}
