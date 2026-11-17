package com.truedata.mobile

import android.app.Activity
import android.content.Context
import android.webkit.JavascriptInterface

class WebAppInterface(private val context: Context, private val pinStore: PinStore) {

    @JavascriptInterface
    fun savePin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return
        pinStore.savePin(pin)
    }

    @JavascriptInterface
    fun isPinSaved(): Boolean = pinStore.isPinSet()

    @JavascriptInterface
    fun clearSavedPin() {
        pinStore.clear()
    }

    @JavascriptInterface
    fun exitApp() {
        val activity = context as? Activity ?: return
        activity.runOnUiThread { activity.finishAffinity() }
    }

    @JavascriptInterface
    fun minimizeApp() {
        val activity = context as? Activity ?: return
        activity.runOnUiThread { activity.moveTaskToBack(true) }
    }
}
