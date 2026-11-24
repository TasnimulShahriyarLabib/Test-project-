package com.labib.reelguard

import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.io.File

class MainActivity : Activity() {

    // Palette
    private val bg = Color.parseColor("#F4F6FB")
    private val primary = Color.parseColor("#5B5BD6")
    private val textDark = Color.parseColor("#1B1F2A")
    private val textMuted = Color.parseColor("#6B7280")
    private val divider = Color.parseColor("#EEF0F5")
    private val green = Color.parseColor("#1E9E5A")
    private val red = Color.parseColor("#E5484D")

    private val look = mapOf(
        "youtube" to Pair("#E62117", "Shorts"),
        "instagram" to Pair("#C13584", "Reels"),
        "facebook" to Pair("#1877F2", "Reels"),
        "tiktok" to Pair("#111111", "Whole app"),
        "snapchat" to Pair("#F2A900", "Spotlight")
    )

    private lateinit var statusCard: LinearLayout
    private lateinit var statusTitle: TextView
    private lateinit var statusSub: TextView
    private lateinit var statusBtn: Button
    private lateinit var counterNum: TextView

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun rounded(color: Int, radiusDp: Int): GradientDrawable {
        val d = GradientDrawable()
        d.setColor(color)
        d.cornerRadius = dp(radiusDp).toFloat()
        return d
    }

    private fun oval(color: Int): GradientDrawable {
        val d = GradientDrawable()
        d.shape = GradientDrawable.OVAL
        d.setColor(color)
        return d
    }

    private fun label(s: String, sp: Float, color: Int, bold: Boolean): TextView {
        val t = TextView(this)
        t.text = s
        t.textSize = sp
        t.setTextColor(color)
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        return t
    }

    private fun card(): LinearLayout {
        val c = LinearLayout(this)
        c.orientation = LinearLayout.VERTICAL
        c.background = rounded(Color.WHITE, 20)
        c.elevation = dp(2).toFloat()
        c.setPadding(dp(18), dp(14), dp(18), dp(14))
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(0, 0, 0, dp(14))
        c.layoutParams = lp
        return c
    }

    private fun pill(text: String, filled: Boolean): Button {
        val b = Button(this)
        b.text = text
        b.isAllCaps = false
        b.stateListAnimator = null
        b.textSize = 15f
        b.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        if (filled) {
            b.background = rounded(primary, 24)
            b.setTextColor(Color.WHITE)
        } else {
            val d = rounded(Color.WHITE, 24)
            d.setStroke(dp(1), primary)
            b.background = d
            b.setTextColor(primary)
        }
        return b
    }

    private fun tint(sw: Switch) {
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        sw.thumbTintList = ColorStateList(
            states, intArrayOf(primary, Color.parseColor("#B0B6C3"))
        )
        sw.trackTintList = ColorStateList(
            states, intArrayOf(Color.parseColor("#C9C8F5"), Color.parseColor("#E1E4EA"))
        )
    }

    private fun appRow(rule: AppRule): LinearLayout {
        val info = look[rule.key] ?: Pair("#5B5BD6", "")
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = Gravity.CENTER_VERTICAL
        row.setPadding(0, dp(10), 0, dp(10))

        val badge = TextView(this)
        badge.text = rule.name.substring(0, 1)
        badge.gravity = Gravity.CENTER
        badge.setTextColor(Color.WHITE)
        badge.textSize = 16f
        badge.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        badge.background = oval(Color.parseColor(info.first))
        row.addView(badge, LinearLayout.LayoutParams(dp(40), dp(40)))

        val names = LinearLayout(this)
        names.orientation = LinearLayout.VERTICAL
        val appName = rule.name.substringBefore(" ").substringBefore("(").trim()
        names.addView(label(appName, 16f, textDark, true))
        names.addView(label("Blocks " + info.second, 13f, textMuted, false))
        val nlp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        nlp.setMargins(dp(14), 0, dp(8), 0)
        row.addView(names, nlp)

        val sw = Switch(this)
        sw.isChecked = Prefs.isEnabled(this, rule.key)
        sw.setOnCheckedChangeListener { _, on -> Prefs.setEnabled(this, rule.key, on) }
        tint(sw)
        row.addView(sw)
        return row
    }

    private fun line(): View {
        val v = View(this)
        v.setBackgroundColor(divider)
        v.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
        )
        return v
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        val col = LinearLayout(this)
        col.orientation = LinearLayout.VERTICAL
        col.setPadding(dp(18), dp(24), dp(18), dp(28))

        // Header
        col.addView(label("ReelGuard", 30f, textDark, true))
        val sub = label("Stay out of the endless scroll", 15f, textMuted, false)
        sub.setPadding(0, dp(2), 0, dp(18))
        col.addView(sub)

        // Status card
        statusCard = LinearLayout(this)
        statusCard.orientation = LinearLayout.VERTICAL
        statusCard.setPadding(dp(22), dp(22), dp(22), dp(22))
        val slp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        slp.setMargins(0, 0, 0, dp(14))
        statusCard.layoutParams = slp
        statusCard.elevation = dp(3).toFloat()
        statusTitle = label("", 22f, Color.WHITE, true)
        statusSub = label("", 14f, Color.parseColor("#E8FFF2"), false)
        statusSub.setPadding(0, dp(4), 0, dp(16))
        statusBtn = Button(this)
        statusBtn.isAllCaps = false
        statusBtn.stateListAnimator = null
        statusBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD)
        statusBtn.background = rounded(Color.WHITE, 24)
        statusBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        statusCard.addView(statusTitle)
        statusCard.addView(statusSub)
        statusCard.addView(statusBtn)
        col.addView(statusCard)

        // Counter card
        val counterCard = card()
        counterCard.addView(label("Blocked today", 13f, textMuted, false))
        counterNum = label("0", 44f, primary, true)
        counterCard.addView(counterNum)
        counterCard.addView(label("times ReelGuard pulled you out of a feed", 13f, textMuted, false))
        col.addView(counterCard)

        // Apps card
        val sectionTitle = label("APPS", 12f, textMuted, true)
        sectionTitle.letterSpacing = 0.1f
        sectionTitle.setPadding(dp(4), dp(6), 0, dp(8))
        col.addView(sectionTitle)

        val appsCard = card()
        val rules = Rules.all
        for (i in rules.indices) {
            appsCard.addView(appRow(rules[i]))
            if (i < rules.size - 1) appsCard.addView(line())
        }
        col.addView(appsCard)

        // Troubleshooting card
        val tTitle = label("TROUBLESHOOTING", 12f, textMuted, true)
        tTitle.letterSpacing = 0.1f
        tTitle.setPadding(dp(4), dp(6), 0, dp(8))
        col.addView(tTitle)

        val tCard = card()
        tCard.addView(label("Capture mode", 16f, textDark, true))
        val tHelp = label(
            "If an app update stops the blocking, turn this on, open its Reels or Shorts " +
                "screen for a few seconds, then tap Share log.",
            13f, textMuted, false
        )
        tHelp.setPadding(0, dp(4), 0, dp(10))
        tCard.addView(tHelp)

        val capture = Switch(this)
        capture.text = "Record screen ids"
        capture.setTextColor(textDark)
        capture.isChecked = Prefs.captureOn(this)
        capture.setOnCheckedChangeListener { _, on -> Prefs.setCapture(this, on) }
        tint(capture)
        tCard.addView(capture)

        val btns = LinearLayout(this)
        btns.orientation = LinearLayout.HORIZONTAL
        btns.setPadding(0, dp(12), 0, 0)
        val share = pill("Share log", true)
        share.setOnClickListener { shareCapture() }
        val clear = pill("Clear", false)
        clear.setOnClickListener {
            File(filesDir, "capture.txt").delete()
            Toast.makeText(this, "Cleared", Toast.LENGTH_SHORT).show()
        }
        val b1 = LinearLayout.LayoutParams(0, dp(48), 1f)
        b1.setMargins(0, 0, dp(8), 0)
        val b2 = LinearLayout.LayoutParams(0, dp(48), 1f)
        b2.setMargins(dp(8), 0, 0, 0)
        btns.addView(share, b1)
        btns.addView(clear, b2)
        tCard.addView(btns)
        col.addView(tCard)

        val scroll = ScrollView(this)
        scroll.setBackgroundColor(bg)
        scroll.clipToPadding = false
        scroll.addView(col)
        setContentView(scroll)
    }

    override fun onResume() {
        super.onResume()
        val on = serviceOn()
        if (on) {
            statusCard.background = rounded(green, 24)
            statusTitle.text = "Protection is ON"
            statusSub.text = "ReelGuard is watching your apps."
            statusBtn.text = "Accessibility settings"
            statusBtn.setTextColor(green)
        } else {
            statusCard.background = rounded(red, 24)
            statusTitle.text = "Protection is OFF"
            statusSub.text = "Turn on ReelGuard in Accessibility settings to start blocking."
            statusBtn.text = "Turn on"
            statusBtn.setTextColor(red)
        }
        counterNum.text = Prefs.blockedToday(this).toString()
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
