package com.labib.reelguard

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import java.io.File

class BlockerService : AccessibilityService() {

    private var lastActionAt = 0L
    private var strikes = 0
    private val seen = HashSet<String>()

    companion object {
        private const val NONE = 0
        private const val TAB = 1     // the Reels/Shorts tab is selected
        private const val VIEWER = 2  // a full-screen Reels/Shorts player is on screen
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        val rule = Rules.forPackage(pkg) ?: return
        if (!Prefs.isEnabled(this, rule.key)) return

        if (rule.blockWholeApp) {
            if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                block(rule, true, null, NONE)
            }
            return
        }

        val root = rootInActiveWindow ?: return
        if (root.packageName?.toString() != pkg) return

        val kind = scan(root, rule, Prefs.captureOn(this))
        if (kind != NONE) block(rule, false, root, kind)
    }

    override fun onInterrupt() {}

    /** Walks the visible view tree (capped) looking for the short-video screen. */
    private fun scan(root: AccessibilityNodeInfo, rule: AppRule, capture: Boolean): Int {
        var kind = NONE
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
                    kind = VIEWER
                }
            }

            if (capture && node.isVisibleToUser) {
                val d = node.contentDescription?.toString()
                if (!d.isNullOrBlank()) record(rule, "desc=" + d.take(50))
                val t = node.text?.toString()
                if (!t.isNullOrBlank()) record(rule, "text=" + t.take(40))
            }

            if (node.isSelected) {
                val label = (node.contentDescription ?: node.text)?.toString()
                if (label != null) {
                    if (capture) record(rule, "selected=" + label.take(60))
                    if (kind == NONE && rule.tabLabels.any { label.startsWith(it, ignoreCase = true) }) {
                        kind = TAB
                    }
                }
            }

            if (kind != NONE && !capture) break

            for (i in 0 until node.childCount) {
                val child = node.getChild(i)
                if (child != null) stack.addLast(child)
            }
        }
        return kind
    }

    /**
     * Opens the app's own "Home" tab. This keeps you inside the app, so it never exits to the
     * launcher and never shrinks a video into a floating window.
     * First tries a normal click; if the tab is not "clickable", taps its position on screen.
     */
    private fun tapHomeTab(root: AccessibilityNodeInfo): Boolean {
        val hits = root.findAccessibilityNodeInfosByText("Home") ?: return false

        var best: AccessibilityNodeInfo? = null
        var bestY = -1
        val r = Rect()
        for (n in hits) {
            val label = (n.contentDescription ?: n.text)?.toString() ?: continue
            if (!label.startsWith("Home", ignoreCase = true) || label.length > 40) continue
            if (!n.isVisibleToUser) continue
            n.getBoundsInScreen(r)
            if (r.isEmpty) continue
            if (r.centerY() > bestY) {
                bestY = r.centerY()
                best = n
            }
        }
        val tab = best ?: return false

        // 1) normal click on the node or its nearest clickable parent
        var target: AccessibilityNodeInfo? = tab
        var depth = 0
        while (target != null && !target.isClickable && depth < 6) {
            target = target.parent
            depth++
        }
        if (target != null && target.isClickable &&
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        ) {
            return true
        }

        // 2) fall back to tapping the tab's position
        tab.getBoundsInScreen(r)
        val path = Path()
        path.moveTo(r.exactCenterX(), r.exactCenterY())
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 60))
            .build()
        return dispatchGesture(gesture, null, null)
    }

    private fun block(rule: AppRule, forceHome: Boolean, root: AccessibilityNodeInfo?, kind: Int) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastActionAt < 1000) return

        strikes = if (now - lastActionAt < 5000) strikes + 1 else 1
        lastActionAt = now

        var acted = true
        if (forceHome) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        } else {
            val tapped = root != null && tapHomeTab(root)
            if (!tapped) {
                // No Home tab found. Back is only safe when a player screen is open on top;
                // when only the tab is selected, Back could exit the whole app, so do nothing.
                if (kind == VIEWER && strikes <= 2) {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                } else {
                    acted = false
                }
            } else if (kind == VIEWER && strikes >= 3) {
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
        }

        if (acted) {
            Prefs.addBlocked(this)
            Toast.makeText(this, rule.name + " blocked", Toast.LENGTH_SHORT).show()
        }
    }

    /** Capture mode: remember which view ids / labels were seen, so rules can be fixed later. */
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
