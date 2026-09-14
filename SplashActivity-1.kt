package com.truedata.mobile

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var pinStore: PinStore

    private val lockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CrashHandler.install(this)
        setContentView(R.layout.activity_splash)
        pinStore = PinStore(this)

        Handler(Looper.getMainLooper()).postDelayed({
            if (pinStore.isPinSet()) {
                lockLauncher.launch(Intent(this, LockActivity::class.java))
            } else {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
        }, 1200)
    }
}
