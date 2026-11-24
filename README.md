# ReelGuard

Android app that blocks Reels / Shorts / Spotlight in Facebook, YouTube, Instagram, Snapchat and blocks TikTok entirely. It uses an Accessibility Service: when a short-video screen is detected it presses Back (and Home if the feed is still there).

## Install
1. Push to `main`. GitHub Actions builds the APK.
2. Open the repo's **Releases > ReelGuard latest** on your phone and download `app-debug.apk`.
3. Install it, open ReelGuard, tap **Open Accessibility settings** and enable ReelGuard.
   On Android 13+ you may first need: Settings > Apps > ReelGuard > three dots > **Allow restricted settings**.

## When an app update breaks detection
Detection rules live in `app/src/main/java/com/labib/reelguard/Rules.kt`.
Turn on **Capture mode** in the app, open the Reels/Shorts screen for a few seconds, tap **Share capture log**, and use the view ids from it to update `Rules.kt`.

## Privacy
Nothing leaves the phone. The service only reads screens of the apps listed in `accessibility_service_config.xml`, and the app has no internet permission.
