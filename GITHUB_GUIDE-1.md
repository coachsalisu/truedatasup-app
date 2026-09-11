# Building TrueData App From Your Phone (No Computer Needed)

This guide replaces the Android Studio steps in the main README, sir — everything
here works from your phone's browser using GitHub.

---

## ⚠️ SAVE THIS PASSWORD SOMEWHERE VERY SAFE, RIGHT NOW

```
7jATQU9oNpJqmzHpbtBp
```

This is your app's permanent signing password. Copy it into your phone's Notes
app, WhatsApp yourself, write it down — anywhere safe, right now, before
continuing. If this is ever lost, you cannot update this app version ever
again; you'd have to publish as a brand new app instead. Anthropic (Claude)
does not keep a copy of this anywhere - this message is the only place it
exists. Once you close this conversation, it's gone from here too.

---

## Step 1 — Create a new PRIVATE repository on GitHub

1. Open github.com in your phone's browser, log in
2. Tap the **+** icon (top right) → **New repository**
3. Name it `truedata-app` (or anything you like)
4. **Important: set it to Private** — not Public. Your source code and app
   setup shouldn't be visible to strangers.
5. Tap **Create repository**

## Step 2 — Upload the project files

1. On your new repo's page, tap **Add file → Upload files**
2. Upload the files from the `TrueDataApp` folder I gave you, matching this
   structure exactly (GitHub will let you type a path like `app/src/main/...`
   before dropping files in, which creates the folders automatically):

   - Files going directly in the root: `build.gradle`, `settings.gradle`,
     `gradle.properties`, `.gitignore`, `README.md`
   - Files going in `app/`: `build.gradle`, `proguard-rules.pro`
   - Files going in `app/src/main/`: `AndroidManifest.xml`
   - Files going in `app/src/main/java/com/truedata/mobile/`: all the `.kt`
     files (MainActivity.kt, LockActivity.kt, PinStore.kt, etc.)
   - Files going in `app/src/main/res/layout/`: all files from that folder
   - Files going in `app/src/main/res/values/`: all files from that folder
   - Files going in `app/src/main/res/drawable/`: all files from that folder
   - Files going in `app/src/main/res/mipmap-anydpi-v26/`: those 2 files
   - Files going in `.github/workflows/`: `build.yml`

   Tip: on the upload page, there's a text field where you can type the
   folder path (e.g. `app/src/main/java/com/truedata/mobile`) - type it once,
   then select/drop all the files for that folder, then repeat for the next
   folder.

3. After each batch, scroll down and tap **Commit changes**

## Step 3 — Add your secret keystore (one-time, very important)

1. On your repo page, tap **Settings** (you may need to tap the "..." menu
   on mobile to find it)
2. Go to **Secrets and variables → Actions**
3. Tap **New repository secret**
4. Name: `TRUEDATA_KEYSTORE_PASSWORD`
   Value: `7jATQU9oNpJqmzHpbtBp` (the password from the top of this guide)
   → Save
5. Tap **New repository secret** again
6. Name: `TRUEDATA_KEYSTORE_BASE64`
   Value: paste the long text block below exactly (this is your actual
   signing key, safely scrambled into text form):

```
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
```

   → Save

## Step 4 — Run the build

1. Go to the **Actions** tab on your repo
2. You should see "Build TrueData App" — tap it
3. Tap **Run workflow** → **Run workflow** (green button)
4. Wait 3-5 minutes (refresh the page) — you'll see a green checkmark ✅ when done
5. Tap into the finished run, scroll down to **Artifacts**
6. Download **TrueData-for-PlayStore** — this is the file you upload to
   Google Play Console
7. Download **TrueData-for-testing** — install this one directly on your own
   phone first, to try the app before publishing (you may need to allow
   "install from unknown sources" for this test install only)

## After this first time

Whenever I give you updated code in the future, you just repeat Step 2
(upload the new/changed files) and Step 4 (run the build again) — Steps 1
and 3 only happen once, ever.

## If something goes wrong

Screenshot whatever error you see on the Actions page (the red ❌ run) and
send it to me - I can read GitHub's build logs the same way I've been
reading your PHP error logs, and tell you exactly what to fix.
