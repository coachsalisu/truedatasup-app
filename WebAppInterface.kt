package com.truedata.mobile

import android.content.Context
import android.webkit.JavascriptInterface
import android.widget.Toast

class WebAppInterface(private val context: Context, private val pinStore: PinStore) {

    /**
     * Called from the website's JavaScript as: Android.savePin("1234")
     * Only ever called right after the site itself has already confirmed
     * the PIN is correct (freshly set, or freshly typed to log in) - this
     * class just stores a local hash of it for fast, offline unlocking.
     */
    @JavascriptInterface
    fun savePin(pin: String) {
        if (pin.length != 4 || !pin.all { it.isDigit() }) return
        pinStore.savePin(pin)
    }

    /** Lets the site check whether this device already has a PIN saved, if it ever needs to. */
    @JavascriptInterface
    fun isPinSaved(): Boolean = pinStore.isPinSet()

    /** Lets the site's own "log out" / "switch account" action also clear the local PIN. */
    @JavascriptInterface
    fun clearSavedPin() {
        pinStore.clear()
    }
}
