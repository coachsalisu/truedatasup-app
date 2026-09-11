package com.truedata.mobile

import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat

class LockActivity : AppCompatActivity() {

    private lateinit var pinStore: PinStore
    private lateinit var dots: List<android.view.View>
    private lateinit var errorText: TextView
    private var enteredPin = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        pinStore = PinStore(this)

        dots = listOf(
            findViewById(R.id.dot0), findViewById(R.id.dot1),
            findViewById(R.id.dot2), findViewById(R.id.dot3)
        )
        errorText = findViewById(R.id.errorText)

        setupNumberPad()
        setupFingerprintKey()
        setupSwitchAccount()

        if (canUseBiometric()) {
            showBiometricPrompt()
        }
    }

    private fun setupNumberPad() {
        val digitIds = mapOf(
            R.id.key0 to "0", R.id.key1 to "1", R.id.key2 to "2", R.id.key3 to "3",
            R.id.key4 to "4", R.id.key5 to "5", R.id.key6 to "6", R.id.key7 to "7",
            R.id.key8 to "8", R.id.key9 to "9"
        )
        digitIds.forEach { (id, digit) ->
            findViewById<Button>(id).setOnClickListener { onDigitTapped(digit) }
        }
        findViewById<ImageButton>(R.id.deleteKey).setOnClickListener { onDeleteTapped() }
    }

    private fun onDigitTapped(digit: String) {
        if (enteredPin.length >= 4) return
        errorText.visibility = android.view.View.INVISIBLE
        enteredPin.append(digit)
        updateDots()
        if (enteredPin.length == 4) {
            checkPin()
        }
    }

    private fun onDeleteTapped() {
        if (enteredPin.isNotEmpty()) {
            enteredPin.deleteCharAt(enteredPin.length - 1)
            updateDots()
        }
    }

    private fun updateDots() {
        dots.forEachIndexed { index, dot ->
            dot.setBackgroundResource(
                if (index < enteredPin.length) R.drawable.pin_dot_filled else R.drawable.pin_dot_empty
            )
        }
    }

    private fun checkPin() {
        if (pinStore.verifyPin(enteredPin.toString())) {
            unlockSuccess()
        } else {
            errorText.visibility = android.view.View.VISIBLE
            enteredPin.clear()
            updateDots()
        }
    }

    private fun unlockSuccess() {
        setResult(RESULT_OK)
        finish()
    }

    private fun canUseBiometric(): Boolean {
        val biometricManager = BiometricManager.from(this)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    private fun setupFingerprintKey() {
        val key = findViewById<ImageButton>(R.id.fingerprintKey)
        key.visibility = if (canUseBiometric()) android.view.View.VISIBLE else android.view.View.INVISIBLE
        key.setOnClickListener { showBiometricPrompt() }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                unlockSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                // User cancelled or too many attempts - just let them fall back to the number pad.
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock TrueDataSUP")
            .setSubtitle("Use your fingerprint to continue")
            .setNegativeButtonText("Use PIN instead")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun setupSwitchAccount() {
        findViewById<TextView>(R.id.switchAccountText).setOnClickListener {
            pinStore.clear()
            setResult(RESULT_FIRST_USER)
            finish()
        }
    }

    override fun onBackPressed() {
        // Don't allow dismissing the lock screen with the back button.
    }
}
