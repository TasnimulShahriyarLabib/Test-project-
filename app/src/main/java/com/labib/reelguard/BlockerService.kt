package com.labib.reelguard

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.File

class BlockerService : AccessibilityService() {

    private var lastBlockAt = 0L
    private var strikes = 0
    private val seen = HashSet<String>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val rule = Rules.forPackage(pkg) ?: return
        if (!Prefs.isEnabled(this, rule.key)) return

        if (rule.blockWholeApp) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                block(rule, true, null)
            }
            return
        }

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        if (scan(root, rule, Prefs.captureOn(this))) {
            block(rule, false, root)
        }
    }

    override fun onInterrupt() {}

    /** Walks the visible view tree (capped) looking for the short-video screen. */
    private fun scan(root: AccessibilityNodeInfo, rule: AppRule, capture: Boolean): Boolean {
        var found = false
        var visited = 0
        val stack = ArrayDeque<AccessibilityNodeInfo>()
        stack.addLast(root)

        while (stack.isNotEmpty() && visited < 800) {
            val node = stack.removeLast()
            visited++

            val id = node.viewIdResourceName
            if (id != null) {
                if (capture) record(rule, "id=" + id)
                val shortId = id.substringAfter(":id/").lowercase()
                if (node.isVisibleToUser && rule.idHints.any { shortId.contains(it) }) {
                    found = true
                }
            }

            if (capture && node.isVisibleToUser) {
                val d = node.contentDescription?.toString()
                if (!d.isNullOrBlank()) record(rule, "desc=" + d.take(50))
            }

            if (node.isSelected) {
                val label = (node.contentDescription ?: node.text)?.toString()
                if (label != null) {
                    if (capture) record(rule, "selected=" + label.take(60))
                    if (rule.tabLabels.any { label.startsWith(it, ignoreCase = true) }) {
                        found = true
                    }
                }
            }

            if (found && !capture) break

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) stack.addLast(child)
            }
        }
        return found
    }

    /**
     * Taps the "Home" tab of the app. Unlike Back/Home buttons this does not leave the app,
     * so YouTube cannot shrink the Short into a floating picture-in-picture window.
     */
    private fun clickHomeTab(root: AccessibilityNodeInfo): Boolean {
        val hits = root.findAccessibilityNodeInfosByText("Home") ?: return false
        for (n in hits) {
            val label = (n.contentDescription ?: n.text)?.toString() ?: continue
            if (!label.startsWith("Home", ignoreCase = true)) continue

            var target: AccessibilityNodeInfo? = n
            var depth = 0
            while (target != null && !target.isClickable && depth < 4) {
                target = target.parent
                depth++
            }
            if (target != null && target.isClickable &&
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
            ) {
                return true
            }
        }
        return false
    }

    private fun block(rule: AppRule, forceHome: Boolean, root: AccessibilityNodeInfo?) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockAt < 800) return

        strikes = if (now - lastBlockAt < 4000) strikes + 1 else 1
        lastBlockAt = now

        if (forceHome) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } else if (strikes == 1) {
            // 1st try: tap the app's Home tab; if it is not there, press Back.
            val tapped = root != null && clickHomeTab(root)
            if (!tapped) performGlobalAction(GLOBAL_ACTION_BACK)
        } else if (strikes == 2) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        } else {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }

        Prefs.addBlocked(this)
        Toast.makeText(this, rule.name + " blocked", Toast.LENGTH_SHORT).show()
    }

    /** Capture mode: remember which view ids / selected tabs were seen, so rules can be fixed later. */
    private fun record(rule: AppRule, text: String) {
        val line = rule.key + " | " + text
        if (seen.size < 3000 && seen.add(line)) {
            try {
                File(filesDir, "capture.txt").appendText(line + "\n")
            } catch (e: Exception) {
                // ignore
            }
        }
    }
}
