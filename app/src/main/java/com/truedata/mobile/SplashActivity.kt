package com.truedata.mobile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.io.StringWriter

class SplashActivity : AppCompatActivity() {

    private lateinit var pinStore: PinStore

    private val lockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            try {
                when (result.resultCode) {
                    RESULT_OK -> {
                        AppLockState.isUnlockedThisSession = true
                        startActivity(Intent(this, DashboardActivity::class.java))
                        finish()
                    }
                    RESULT_FIRST_USER -> {
                        // "Switch account" was tapped on the lock screen - PIN was cleared.
                        startActivity(Intent(this, WelcomeActivity::class.java))
                        finish()
                    }
                    else -> finish()
                }
            } catch (e: Throwable) {
                showCrash(e)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.install(this)
        try {
            setContentView(R.layout.activity_splash)
            pinStore = PinStore(this)

            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    if (pinStore.isPinSet()) {
                        lockLauncher.launch(Intent(this, LockActivity::class.java))
                    } else {
                        startActivity(Intent(this, WelcomeActivity::class.java))
                        finish()
                    }
                } catch (e: Throwable) {
                    showCrash(e)
                }
            }, 1200)
        } catch (e: Throwable) {
            showCrash(e)
        }
    }

    private fun showCrash(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        AlertDialog.Builder(this)
            .setTitle("App crashed here")
            .setMessage(sw.toString())
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ -> finishAffinity() }
            .show()
    }
}
