package com.labib.reelguard

/**
 * One entry per app. This is the file to edit when an app update breaks detection.
 *
 * idHints   - lowercase fragments of a view id (the part after ":id/"). If a VISIBLE view
 *             whose id contains one of these is on screen, the app is showing a short-video feed.
 * tabLabels - if a SELECTED view (a bottom/top tab) has a text or content description that
 *             starts with one of these, the short-video tab is open.
 * blockWholeApp - for apps that are only short video (TikTok): go home as soon as it opens.
 */
data class AppRule(
    val key: String,
    val name: String,
    val packages: Set<String>,
    val blockWholeApp: Boolean,
    val idHints: List<String>,
    val tabLabels: List<String>
)

object Rules {
    val all: List<AppRule> = listOf(
        AppRule(
            key = "youtube",
            name = "YouTube Shorts",
            packages = setOf("com.google.android.youtube"),
            blockWholeApp = false,
            idHints = listOf("reel_player_page_container", "reel_recycler", "shorts_player"),
            tabLabels = listOf("Shorts")
        ),
        AppRule(
            key = "instagram",
            name = "Instagram Reels",
            packages = setOf("com.instagram.android"),
            blockWholeApp = false,
            idHints = listOf("clips_viewer", "clips_video_container"),
            tabLabels = listOf("Reels")
        ),
        AppRule(
            key = "facebook",
            name = "Facebook Reels",
            packages = setOf("com.facebook.katana", "com.facebook.lite"),
            blockWholeApp = false,
            idHints = listOf("reels_viewer", "reel_viewer"),
            tabLabels = listOf("Reels")
        ),
        AppRule(
            key = "tiktok",
            name = "TikTok (whole app)",
            packages = setOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill"),
            blockWholeApp = true,
            idHints = emptyList(),
            tabLabels = emptyList()
        ),
        AppRule(
            key = "snapchat",
            name = "Snapchat Spotlight",
            packages = setOf("com.snapchat.android"),
            blockWholeApp = false,
            idHints = listOf("spotlight"),
            tabLabels = listOf("Spotlight")
        )
    )

    fun forPackage(pkg: String): AppRule? = all.firstOrNull { pkg in it.packages }
}
