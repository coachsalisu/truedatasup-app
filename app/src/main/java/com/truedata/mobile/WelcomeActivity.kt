package com.truedata.mobile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class WelcomeActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }
}
