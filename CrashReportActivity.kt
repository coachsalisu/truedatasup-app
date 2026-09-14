package com.truedata.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class CrashReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crash_report)

        val prefs = getSharedPreferences("truedata_crash", MODE_PRIVATE)
        val crashText = prefs.getString("last_crash", "No crash details were saved.")

        findViewById<TextView>(R.id.crashText).text = crashText
        findViewById<Button>(R.id.closeButton).setOnClickListener {
            finishAffinity()
        }
    }
}
