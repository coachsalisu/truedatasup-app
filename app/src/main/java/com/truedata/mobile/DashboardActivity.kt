package com.truedata.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class DashboardActivity : AppCompatActivity() {

    private lateinit var pinStore: PinStore

    private val lockLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            when (result.resultCode) {
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
        setContentView(R.layout.activity_dashboard)
        pinStore = PinStore(this)

        openFeature(R.id.cardData, "home/buy-data")
        openFeature(R.id.cardAirtime, "home/buy-airtime")
        openFeature(R.id.cardCable, "home/cable-tv")
        openFeature(R.id.cardElectricity, "home/electricity")
        openFeature(R.id.cardEduPins, "home/exam-pins")
        openFeature(R.id.cardBulkSms, "home/bulk-sms")
        
