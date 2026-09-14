package com.truedata.mobile

import android.content.Context
import android.content.Intent
import java.io.PrintWriter
import java.io.StringWriter

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val crashText = sw.toString()

            val prefs = context.getSharedPreferences("truedata_crash", Context.MODE_PRIVATE)
            prefs.edit().putString("last_crash", crashText).apply()

            val intent = Intent(context, CrashReportActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            context.startActivity(intent)
        } catch (e: Exception) {
            // If even the crash handler fails, fall back to the system default.
        }

        // Give the new activity a moment to launch, then actually end this process.
        Runtime.getRuntime().exit(1)
    }

    companion object {
        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }
    }
}
