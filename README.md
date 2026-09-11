# TrueData Android App — Setup Guide (for Coach)

This is a complete WebView-based Android app project for TrueData. It loads
`https://truedata.com.ng/mobile` inside the app, and includes:

- Splash screen (brand color + logo, 1.2s)
- Pull-to-refresh
- Offline mode: after the dashboard has loaded once while online, it keeps
  working with no internet - it shows the last saved version with a clear
  red "You're offline - showing saved data" banner. A full "No Internet"
  screen only appears if a page has never loaded before (e.g. very first
  launch with no connection).
- File upload support (for KYC photo/document uploads on your site)
- File download support (for receipts etc, saved to the phone's Downloads folder)
- Push notifications (needs a one-time Firebase setup — Step 6 below)

You do **not** need any coding experience to get this running — just follow
the steps below in order.

---

## Step 1 — Install Android Studio

1. Go to https://developer.android.com/studio and download Android Studio for
   your operating system (Windows/Mac/Linux).
2. Install it and open it. On first launch, let it download the Android SDK
   (it will prompt you — just click Next/Finish, this can take 10-20 minutes
   depending on your internet).

## Step 2 — Open this project

1. Unzip the file I gave you (`TrueDataApp.zip`) somewhere on your computer,
   e.g. `Documents/TrueDataApp`.
2. Open Android Studio → **File > Open** → select the unzipped `TrueDataApp`
   folder → Open.
3. Android Studio will now "Sync" the project (downloading the pieces it
   needs). This can take a few minutes the first time — just wait for the
   progress bar at the bottom to finish. If it asks to update Gradle or the
   Android Gradle Plugin, click **Yes**.

If Sync fails with a Gradle-version error, that's fine — just let Android
Studio's own prompt ("Upgrade Gradle version") fix it automatically; it knows
which version matches the Android Studio version you installed.

## Step 3 — Run it on your phone (test before publishing)

1. On your Android phone: go to Settings > About Phone > tap "Build Number"
   7 times to unlock Developer Options. Then go to Settings > Developer
   Options > turn on **USB Debugging**.
2. Plug your phone into your computer with a USB cable. Allow the "Allow USB
   debugging?" popup on your phone.
3. In Android Studio, your phone's name should appear in the device dropdown
   at the top. Click the green **Run ▶** button.
4. The app installs and opens on your phone, loading truedata.com.ng/mobile.

## Step 4 — Replace the placeholder logo/icon (optional but recommended)

The app currently ships with a simple placeholder "T" icon in your brand
purple. To use your real logo:

1. In Android Studio, right-click the `app` folder → **New > Image Asset**.
2. Choose your logo PNG file (ideally a square image, at least 512x512px,
   transparent background works best).
3. Click Next → Finish. This automatically replaces the app icon everywhere.

## Step 5 — Change the brand color (optional)

Open `app/src/main/res/values/colors.xml` and change the hex value next to
`brand_purple` to match your exact brand color.

## Step 6 — Push notifications setup (optional, do this when ready)

Push notifications need a free Firebase account connected to the app:

1. Go to https://console.firebase.google.com → Create a project (any name,
   e.g. "TrueData").
2. Inside the project, click **Add app > Android**.
3. For "Android package name", enter exactly: `com.truedata.mobile`
4. Download the `google-services.json` file it gives you.
5. Copy that file into the `app/` folder of this project (same folder as
   `build.gradle` — NOT the root folder).
6. Open `build.gradle` (the one inside `app/` folder) and remove the `//` in
   front of this line so it becomes active:
   `id 'com.google.gms.google-services'`
7. Click **Sync Now** when Android Studio prompts you.
8. Push notifications will now work. To actually send one, you can use the
   Firebase Console (Engage > Messaging) to send test messages manually, or
   later have your PHP backend call the Firebase API directly to notify
   customers automatically (e.g. "your data purchase was successful").

You can skip this step entirely for now and the app will still work fine —
it just won't receive push notifications until you complete it.

## Step 7 — Build the file to upload to Play Store

1. In Android Studio: **Build > Generate Signed App Bundle / APK**.
2. Choose **Android App Bundle** (this is what Play Store wants).
3. Click **Create new...** to make your signing key (this is a password-
   protected file that proves the app updates are really from you — save
   this file and its passwords somewhere very safe, you'll need the EXACT
   same key for every future update, or Play Store will reject it).
4. Fill in the key details (any name/organization info is fine), set a
   strong password, click OK, then Next, then Finish.
5. Android Studio creates a `.aab` file — this is what you upload to
   Google Play Console (https://play.google.com/console) under your app's
   "Production" or "Testing" release section.

## Notes

- **Offline mode is safe by design**: purchases, login, and every other form
  submission always require a live connection - they are never faked or
  served from the offline cache. Only pages you view (GET requests, e.g. the
  dashboard, history) get saved for offline viewing, and always with a
  visible banner showing when that data was last saved, so a customer can
  never mistake an old balance for a live one.
- The app only opens links that contain `truedata.com.ng` inside itself;
  any other link (e.g. WhatsApp, a payment gateway page) opens in the
  phone's normal browser/app instead — this is standard and expected
  behavior for WebView apps.
- If you ever change your site's URL structure, update `targetUrl` in
  `app/src/main/java/com/truedata/mobile/MainActivity.kt` and the
  `target_url` string in `app/src/main/res/values/strings.xml`.
