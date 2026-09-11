# Building TrueData SUP App From Your Phone (No Computer Needed)

This guide replaces the Android Studio steps in the main README, sir — everything
here works from your phone's browser using GitHub. This version reflects the
exact method that actually worked for you, uploading `truedatasup-app`.

---

## ✅ Your repo is already set up

`github.com/coachsalisu/truedatasup-app` — all 57 files are already uploaded
and correctly placed. You don't need to redo the upload steps below unless
starting completely fresh. Skip straight to **"Updating a file later"** near
the bottom for anything after today.

---

## ⚠️ Your signing password

```
7jATQU9oNpJqmzHpbtBp
```

This is your app's permanent signing password — already saved as a GitHub
Secret in your repo. Also keep a copy somewhere of your own (Notes app,
WhatsApp yourself) — if this is ever lost, you cannot update this app version
ever again; you'd have to publish as a brand new app instead.

---

## The method that actually works, sir (for reference / starting fresh)

GitHub's "type a folder path first" box is unreliable on mobile — don't use
it. Instead, use this two-step trick for every file that needs to go inside
a folder:

1. Go to `github.com/coachsalisu/truedatasup-app/upload/main`
2. Tap **"choose your files"**, select the file(s) - they'll upload to the
   root of the repo (that's fine, temporary)
3. Tap **Commit changes**
4. Tap on the uploaded file → tap the **pencil/edit icon**
5. At the top, where the filename is shown, **add the full folder path in
   front of the filename** - e.g. change `MainActivity.kt` to
   `app/src/main/java/com/truedata/mobile/MainActivity.kt`
6. Tap **Commit changes** again - this actually **moves** the file into a
   new folder, creating every folder in that path automatically

This is slower than a real desktop drag-and-drop, but it's the version that
reliably works from a phone browser.

## Where each part of the project goes

- Root: `build.gradle`, `settings.gradle`, `gradle.properties`, `.gitignore`,
  `GITHUB_GUIDE.md`, `README.md`
- `app/`: `build.gradle`, `proguard-rules.pro`
- `app/src/main/`: `AndroidManifest.xml`
- `app/src/main/java/com/truedata/mobile/`: all 11 `.kt` files
- `app/src/main/res/layout/`: all 6 `activity_*.xml` files
- `app/src/main/res/values/`: `colors.xml`, `strings.xml`, `themes.xml`
- `app/src/main/res/drawable/`: all 25 icon/background `.xml` files
- `app/src/main/res/mipmap-anydpi-v26/`: `ic_launcher.xml`, `ic_launcher_round.xml`
- `.github/workflows/`: `build.yml`

## Running the build

1. Go to the **Actions** tab on your repo
2. Tap **"Build TrueData App"**
3. Tap **Run workflow** → **Run workflow** (green button)
4. Wait 3-5 minutes (refresh the page) — look for a green checkmark ✅
5. Tap into the finished run, scroll down to **Artifacts**
6. Download **TrueData-for-PlayStore** — this is what you upload to Google
   Play Console
7. Download **TrueData-for-testing** — install this one on your own phone
   first to try the app before publishing (you may need to allow "install
   from unknown sources" for this one test install)

## Updating a file later (the normal case going forward)

Almost every future change is just **1-3 files**, not the whole project:

1. I'll tell you exactly which file(s) changed and give them to you
2. Upload each one the same upload-then-rename way described above - but
   since the folder already exists this time, GitHub will often let you
   overwrite the existing file directly if you upload with the right name
   already, or you can still use the rename trick if needed
3. Once uploaded, go back to **Running the build** above and run it again

## If something goes wrong

Screenshot whatever error you see on the Actions page (the red ❌ run) and
send it to me - I can read GitHub's build logs the same way I've been
reading your PHP error logs, and tell you exactly what to fix.
