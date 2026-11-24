package com.labib.reelguard

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    private lateinit var status: TextView
    private lateinit var counter: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(pad, pad, pad, pad)

        status = TextView(this)
        status.textSize = 18f
        counter = TextView(this)
        counter.setPadding(0, pad / 2, 0, pad / 2)

        val open = Button(this)
        open.text = "Open Accessibility settings"
        open.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        col.addView(status)
        col.addView(counter)
        col.addView(open)

        for (rule in Rules.all) {
            val sw = Switch(this)
            sw.text = rule.name
            sw.textSize = 16f
            sw.setPadding(0, pad / 2, 0, pad / 2)
            sw.isChecked = Prefs.isEnabled(this, rule.key)
            sw.setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this, rule.key, on) }
            col.addView(sw)
        }

        val captureHelp = TextView(this)
        captureHelp.text = "\nCapture mode (for fixing detection after an app update): " +
            "turn it on, open the Reels/Shorts screen for a few seconds, then tap Share."
        col.addView(captureHelp)

        val capture = Switch(this)
        capture.text = "Capture mode"
        capture.isChecked = Prefs.captureOn(this)
        capture.setOnCheckedChangeListener { _, on -> Prefs.setCapture(this, on) }
        col.addView(capture)

        val share = Button(this)
        share.text = "Share capture log"
        share.setOnClickListener { shareCapture() }
        col.addView(share)

        val clear = Button(this)
        clear.text = "Clear capture log"
        clear.setOnClickListener {
            File(filesDir, "capture.txt").delete()
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
        }
        col.addView(clear)

        val scroll = ScrollView(this)
        scroll.addView(col)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        val on = serviceOn()
        status.text = if (on) "Protection is ON" else "Protection is OFF - enable ReelGuard in Accessibility settings"
        counter.text = "Blocked today: " + Prefs.blockedToday(this)
    }

    private fun serviceOn(): Boolean {
        val enabled = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(packageName + "/") && enabled.contains("BlockerService")
    }

    private fun shareCapture() {
        val f = File(filesDir, "capture.txt")
        val text = if (f.exists()) {
            f.readLines().distinct().joinToString("\n").takeLast(60000)
        } else {
            "(empty)"
        }
        val send = Intent(Intent.ACTION_SEND)
        send.type = "text/plain"
        send.putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, "Share capture log"))
    }
}
