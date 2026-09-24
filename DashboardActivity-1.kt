package com.truedata.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var pinStore: PinStore

    companion object {
        private const val LOCK_REQUEST_CODE = 1002
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != LOCK_REQUEST_CODE) return
        when (resultCode) {
            RESULT_OK -> AppLockState.isUnlockedThisSession = true
            RESULT_FIRST_USER -> {
                startActivity(Intent(this, WelcomeActivity::class.java))
                finish()
            }
            else -> finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_dashboard)
            pinStore = PinStore(this)

            openFeature(R.id.cardData, "home/buy-data")
            openFeature(R.id.cardAirtime, "home/buy-airtime")
            openFeature(R.id.cardCable, "home/cable-tv")
            openFeature(R.id.cardElectricity, "home/electricity")
            openFeature(R.id.cardEduPins, "home/exam-pins")
            openFeature(R.id.cardBulkSms, "home/bulk-sms")
            openFeature(R.id.cardRechargePin, "home/recharge-pin")
            openFeature(R.id.cardAirtimeSwap, "home/airtime-to-cash")
            openFeature(R.id.cardWithdraw, "home/withdraw-to-bank")

            openFeature(R.id.navHistory, "home/transactions")

            findViewById<LinearLayout>(R.id.navMe).setOnClickListener {
                startActivity(Intent(this, MeActivity::class.java))
            }
            // navHome does nothing - we're already here.
        } catch (e: Throwable) {
            val sw = java.io.StringWriter()
            e.printStackTrace(java.io.PrintWriter(sw))
            android.app.AlertDialog.Builder(this)
                .setTitle("Dashboard crashed")
                .setMessage(sw.toString())
                .setCancelable(false)
                .setPositiveButton("Close") { _, _ -> finishAffinity() }
                .show()
        }
    }

    private fun openFeature(viewId: Int, path: String) {
        findViewById<LinearLayout>(viewId).setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("TARGET_PATH", path)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        try {
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

    override fun onStop() {
        super.onStop()
        AppLockState.isUnlockedThisSession = false
    }
}
