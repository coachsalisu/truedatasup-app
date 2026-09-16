package com.truedata.mobile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import java.io.PrintWriter
import java.io.StringWriter

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_welcome)

            findViewById<Button>(R.id.registerButton).setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("TARGET_PATH", "register/")
                startActivity(intent)
            }

            findViewById<Button>(R.id.loginButton).setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("TARGET_PATH", "login/")
                startActivity(intent)
            }
        } catch (e: Throwable) {
            showCrash(e)
        }
    }

    private fun showCrash(e: Throwable) {
        val sw = StringWriter()
        e.printStackTrace(PrintWriter(sw))
        AlertDialog.Builder(this)
            .setTitle("Welcome screen crashed")
            .setMessage(sw.toString())
            .setCancelable(false)
            .setPositiveButton("Close") { _, _ -> finishAffinity() }
            .show()
    }
}
