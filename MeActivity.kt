package com.truedata.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class MeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_me)

        findViewById<ImageButton>(R.id.backButton).setOnClickListener { finish() }

        openPage(R.id.menuProfile, "home/profile")

        // TODO CONFIRM WITH COACH: not certain this is the right page for
        // account tier / vendor-agent upgrades - using home/pricing as a
        // placeholder until confirmed.
        openPage(R.id.menuUpgrade, "home/pricing")

        openPage(R.id.menuSupport, "home/support")

        openPage(R.id.menuDeleteAccount, "home/delete-account")

        findViewById<LinearLayout>(R.id.menuLogout).setOnClickListener {
            PinStore(this).clear()
            val intent = Intent(this, WelcomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
    }

    private fun openPage(viewId: Int, path: String) {
        findViewById<LinearLayout>(viewId).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("TARGET_PATH", path)
            startActivity(intent)
        }
    }
}
