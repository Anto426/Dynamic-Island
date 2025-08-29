# Material You Dynamic Island v1.2.0

## 🚀 Release Notes

This release introduces a complete overhaul of the update system with automatic checking, enhanced UI notifications, and improved user experience.

## 📦 Downloads

Choose the appropriate file for your needs:

### For Development/Testing
- **Debug APK**: `MaterialYou-Dynamic-Island-v1.2.0-debug.apk` (84.6 MB)
  - Includes debug symbols and logging
  - Not optimized for production use

### For Production/Release
- **Release APK**: `MaterialYou-Dynamic-Island-v1.2.0-release-unsigned.apk` (64.9 MB)
  - Optimized and minified
  - Requires manual signing before distribution
- **Android App Bundle**: `MaterialYou-Dynamic-Island-v1.2.0-bundle.aab` (20.1 MB)
  - Recommended for Google Play Store
  - Smaller size due to dynamic delivery

## 🔧 Installation Instructions

### APK Installation (Debug/Release)
1. Enable "Install from unknown sources" in Android settings
2. Transfer the APK file to your device
3. Open the APK file and follow the installation prompts
4. Launch the app from your app drawer

### Google Play Store (Bundle)
1. Upload the `.aab` file to Google Play Console
2. Configure release tracks and testing
3. Publish to your desired audience

## ✨ New Features

### 🔄 Automatic Update System
- **Startup Check**: Automatically checks for updates when app launches
- **Persistent State**: Remembers update availability across app restarts
- **Smart Intervals**: Checks every 6 hours to avoid excessive API calls
- **User Control**: Respects user preferences for auto-update behavior

### 🎨 Enhanced UI
- **Prominent Notifications**: Large, eye-catching update cards
- **Navigation Badges**: Red badges on settings icon when updates available
- **Smooth Animations**: Material You compliant transitions
- **Better Typography**: Improved readability and visual hierarchy

### 🛠️ Technical Improvements
- **Modern Architecture**: MVVM pattern with proper state management
- **Coroutine Safety**: Replaced deprecated GlobalScope with proper scopes
- **Error Handling**: Comprehensive error management and user feedback
- **Performance**: Optimized background operations and memory usage

## 🔒 Security & Privacy

- **Secure Downloads**: HTTPS-only update checks
- **Permission Management**: Minimal required permissions
- **Data Validation**: Strict validation of update information
- **User Consent**: Clear opt-in for update checking

## 🐛 Known Issues

- Some deprecated API warnings in build log (non-critical)
- Update system requires internet connection
- Manual APK signing required for release distribution

## 📋 System Requirements

- **Minimum Android Version**: Android 14 (API 34)
- **Recommended Android Version**: Android 15+ (API 35+)
- **Architecture**: ARM64, ARM32, x86_64
- **Storage**: ~200 MB free space for installation

## 🔄 Migration Guide

### From v1.1.0
No special migration steps required. The app will:
- Automatically detect and migrate existing settings
- Enable update checking on first launch (user can disable)
- Preserve all existing customization and preferences

### Update Configuration
The app now uses GitHub releases for update information:
- Stable channel: `https://raw.githubusercontent.com/Anto426/Dynamic-Island/main/release/stable.json`
- Beta channel: `https://raw.githubusercontent.com/Anto426/Dynamic-Island/main/release/beta.json`
- Alpha channel: `https://raw.githubusercontent.com/Anto426/Dynamic-Island/main/release/alpha.json`

## 📞 Support

For issues or questions:
- Create an issue on GitHub
- Check the troubleshooting section in app settings
- Review the changelog for known issues

## 📊 Build Information

- **Build Date**: August 29, 2025
- **Commit**: `aab2ce7`
- **Version Code**: 8
- **Version Name**: 2.1.0
- **Build Type**: Release
- **Minification**: Enabled
- **ProGuard**: Basic optimization

---

**Checksums** (for verification):
- Debug APK: `247C2D83E3FCCCD8DC1736134121AE504B31AFEC53B19FA926FA210BCF60072B`
- Release APK: `33FD8E4FFE8117374167D2AB7F418241CD217C90235E7EAA49217CEBCE2DDF5C`
- Bundle AAB: `A607E5097A3656917C9D4A7B814E519DCF1A6AE878EAD3C2A9E3CCD062E3DD6F`

*Built with ❤️ using Android Studio and Kotlin*
