# Material You Dynamic Island

An Android app that brings a customizable, iOS-style Dynamic Island with Material You design. Easy setup, multi-language UI, and in-app updates.

Current version: 2.1.5 (code 12)

## 📥 Download

- Stable download (APK):
  - Direct link: https://github.com/Anto426/Dynamic-Island/raw/refs/heads/main/release/res/app-debug.apk
  - If the link doesn’t work, browse APKs and grab the latest: https://github.com/Anto426/Dynamic-Island/tree/main/release/res
- In-app updates: Settings → Updates (channels: Stable, Beta, Alpha)

## 🧰 What it can do

- Shows the island over apps, including lock screen
- Notifications with quick actions and replies
- Media controls (play/pause, previous/next, album art)
- Battery info (charging, saver, percentage)
- Material You theme and localized interface (EN, IT, and more)

## 🖼️ Screenshots

If you cloned the repo, place PNGs in `release/screenshots/` using the suggested names. The README will automatically pick them up via these relative links (add files to see them here):

- Overview: ![Overview](release/screenshots/01_overview.png)
- Notifications: ![Notifications](release/screenshots/02_notifications.png)
- Media controls: ![Media](release/screenshots/03_media.png)
- Battery plugin: ![Battery](release/screenshots/04_battery.png)
- Settings & Updates: ![Settings](release/screenshots/05_settings.png)

## ✅ Requirements

- Android 14 or newer (minSdk 34)
- Required permissions: Accessibility, Display over other apps, Notifications (where needed), Install unknown apps (for in-app updates)

## 📲 Install (APK)

1. Download the APK from the link above
2. On Android, enable “Install unknown apps” for the app you use to open the file (e.g., Browser or Files)
3. Open the APK and tap Install
4. Launch the app and follow the short setup

## 🔐 Permissions to enable on first launch

- Accessibility Service: to show and interact with the island
- Display over other apps (Overlay): to appear on top
- Disable battery optimization (recommended): to avoid background kills
- Notifications and Notification Access: for notification/update features
- Allow installing unknown apps: to install updates downloaded by the app

## 🔄 Updates

- In-app: Settings → Updates
- Pick a channel (Stable/Beta/Alpha), then check and download
- Since version 2.1.5, checksum and file-size verification are no longer used

## 🧩 Technical details (brief)

- Tech stack: Kotlin, Jetpack Compose (Material 3), Coroutines/Flows
- Overlay: a foreground service draws the island above apps; lock screen support
- Plugins: Battery, Notifications, Media. Each plugin exposes localized name/description from resources
- Settings: reactive state with debounced broadcasts to the overlay service for live updates
- Updater: reads channel JSONs under `release/*.json` and downloads APK via a simplified `DownloadManager` (no checksum/file-size)
- Min/Target SDK: minSdk 34; target/latest per Gradle config

## 🔎 Privacy

Dynamic Island does not collect or sell personal data. Permissions are used only for the described features (overlay, notifications, updates).

## 📣 Support

- Open an Issue on GitHub: https://github.com/Anto426/Dynamic-Island/issues
- See the changelog: `CHANGELOG.md`
- Release notes: `RELEASE_NOTES.md`

Thanks for using Dynamic Island! 💜


