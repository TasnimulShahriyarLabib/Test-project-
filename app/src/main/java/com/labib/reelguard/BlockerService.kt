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
                block(rule, forceHome = true)
            }
            return
        }

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        if (scan(root, rule, Prefs.captureOn(this))) {
            block(rule, forceHome = false)
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

    private fun block(rule: AppRule, forceHome: Boolean) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockAt < 800) return

        strikes = if (now - lastBlockAt < 4000) strikes + 1 else 1
        lastBlockAt = now

        // First try Back; if the feed is still there right after, go Home.
        val goHome = forceHome || strikes >= 2
        performGlobalAction(if (goHome) GLOBAL_ACTION_HOME else GLOBAL_ACTION_BACK)

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
