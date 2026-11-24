package com.labib.reelguard

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("reelguard", Context.MODE_PRIVATE)

    private fun today(): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    fun isEnabled(c: Context, key: String): Boolean = sp(c).getBoolean("on_$key", true)

    fun setEnabled(c: Context, key: String, value: Boolean) {
        sp(c).edit().putBoolean("on_$key", value).apply()
    }

    fun captureOn(c: Context): Boolean = sp(c).getBoolean("capture", false)

    fun setCapture(c: Context, value: Boolean) {
        sp(c).edit().putBoolean("capture", value).apply()
    }

    fun blockedToday(c: Context): Int {
        val s = sp(c)
        return if (s.getString("day", "") == today()) s.getInt("count", 0) else 0
    }

    fun addBlocked(c: Context) {
        val s = sp(c)
        val n = blockedToday(c)
        s.edit().putString("day", today()).putInt("count", n + 1).apply()
    }
}
