# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.1.2] - 2025-08-30

### 🐛 Fixed
- **Download and Installation Issues**
  - Fixed permission errors (EACCES) by adding fallback to private external storage
  - Improved checksum validation to skip invalid placeholder files
  - Enhanced error handling for download failures
  - Added automatic installation after successful download
  - Implemented cleanup of old APK versions

### 🔧 Changed
- **Update System Improvements**
  - Updated UI to show "Installa Aggiornamento" button after download
  - Added explicit URI permissions for APK installation
  - Improved FileProvider configuration for secure file sharing
  - Enhanced progress tracking and user feedback

### ✨ Added
- **New Features**
  - Automatic APK cleanup on version updates
  - Better error messages for download failures
  - Localized strings for install button in all languages

## [2.1.1] - 2025-08-29

### ✨ Added
- **Complete Update System Overhaul**
  - Automatic update checking on app startup
  - Persistent update state management with SharedPreferences
  - Enhanced UI with prominent update notifications
  - Navigation bar badge for available updates
  - Improved update information display with better visual hierarchy

- **New Core Components**
  - `UpdateViewModel` - Manages update state and UI interactions
  - `LocalUpdateManager` - Handles update checks from GitHub releases
  - `DownloadManager` - Manages APK downloads with progress tracking
  - `UpdateSettingsScreen` - Dedicated settings page for update management

- **Visual Enhancements**
  - Animated update status cards with Material You design
  - Badge indicators in navigation bar for update notifications
  - Improved typography and spacing in update screens
  - Smooth transitions and micro-interactions

### 🔧 Changed
- **Build Configuration**
  - Updated version code to 8
  - Updated version name to 2.1.0
  - Added update-related dependencies
  - Configured AndroidManifest for update permissions

- **Code Architecture**
  - Migrated from deprecated `GlobalScope` to `CoroutineScope`
  - Replaced deprecated `TopAppBarDefaults.centerAlignedTopAppBarColors`
  - Fixed import conflicts between navigation and island packages
  - Improved error handling and lifecycle management

### 🐛 Fixed
- **Compilation Errors**
  - Resolved missing parameters in `UpdateInfo` constructor
  - Fixed unused parameters in `UpdateSettingsScreen`
  - Corrected import conflicts and deprecated API usage
  - Improved parameter validation in update dialogs

- **Performance Improvements**
  - Better memory management with proper coroutine scopes
  - Optimized update checking with 6-hour intervals
  - Reduced unnecessary API calls and background processes

### 📦 Build Artifacts
- **Debug APK**: `MaterialYou-Dynamic-Island-v1.2.0-debug.apk` (84.6 MB)
- **Release APK**: `MaterialYou-Dynamic-Island-v1.2.0-release-unsigned.apk` (64.9 MB)
- **Android App Bundle**: `MaterialYou-Dynamic-Island-v1.2.0-bundle.aab` (20.1 MB)

### 🔒 Security
- Enhanced permission handling for update downloads
- Improved data validation for update information
- Better error handling for network operations

### 📋 Technical Details
- **Minimum SDK**: Android 14 (API 34)
- **Target SDK**: Android 16 (API 36)
- **Kotlin Version**: 2.0.0
- **Compose BOM**: 2024.06.00

---

## Previous Versions

### [1.1.0] - 2025-08-XX
- Initial release with basic Dynamic Island functionality
- Material You theming implementation
- Plugin system architecture
- Basic settings and customization options

---

## Development Notes

### Update System Architecture
The new update system consists of three main components:

1. **LocalUpdateManager**: Handles fetching update information from GitHub releases
2. **UpdateViewModel**: Manages UI state and user interactions
3. **DownloadManager**: Handles secure APK downloads with progress tracking

### File Structure
```
app/src/main/java/com/anto426/dynamicisland/updater/
├── UpdateViewModel.kt          # UI state management
├── LocalUpdateManager.kt       # Update checking logic
├── DownloadManager.kt          # Download handling
└── UpdateManager.kt            # Legacy update system (deprecated)
```

### Configuration Files
- Update information stored in JSON format on GitHub
- Support for multiple release channels (stable, beta, alpha)
- Configurable update check intervals and user preferences

---

*For more information about the update system, see the [Update System Documentation](./docs/UPDATE_SYSTEM.md)*
