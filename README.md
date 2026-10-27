# FeedCleaner for LinkedIn — Android MVP

A small Android app that opens LinkedIn in an embedded WebView and applies local, configurable text-pattern filters to feed posts.

## Features
- Filtering on/off switch
- Strict mode for extra formatting-pattern heuristics
- Optional engagement-bait filters
- Reload feed and restore hidden posts
- Heuristic processing runs locally in the WebView; this app does not call an AI detection API

## Important limitations
- This does **not** prove that a post was written by AI. It hides posts matching selected phrases or style patterns, so human-written posts can be hidden and AI-written posts can pass through.
- LinkedIn can change its markup, causing selectors to stop matching. The embedded WebView may also behave differently from the official LinkedIn app or Chrome, and LinkedIn may restrict embedded login experiences.
- This is an unverified MVP. Test it with a secondary account first. Do not enter credentials if the embedded login screen looks unexpected.
- No analytics, ad SDK, or external filtering service is included. LinkedIn itself receives normal requests when you browse its site.

## Build with Android Studio
1. Install Android Studio with Android SDK Platform 35 and a compatible JDK.
2. Open this folder as an existing project.
3. Let Gradle sync, then choose **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
4. The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK through GitHub Actions
1. Create a GitHub repository and upload this project to its `main` branch.
2. Open **Actions** and run **Build FeedCleaner APK** (or push a commit).
3. Download the `FeedCleaner-debug-apk` artifact from the completed workflow run.

The workflow builds a debug APK for testing. A production release should be signed with your own private signing key before distribution.
