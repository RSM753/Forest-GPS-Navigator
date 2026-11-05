# Forest GPS Navigator (Android source project)

## Included
- Import a PDF map and copy it into app-private storage for offline viewing
- Render PDF pages using Android PdfRenderer
- Display live GPS latitude/longitude, accuracy and altitude
- Record GPS track points to CSV
- Save waypoints to CSV
- No Avenza code, licensing, or services are used

## Build
1. Install Android Studio (Jellyfish or newer) and Android SDK 35.
2. Open this folder as a project.
3. Allow Gradle sync.
4. Connect the Vivo phone with USB debugging enabled, or build an APK via Build > Build Bundle(s) / APK(s) > Build APK(s).
5. Grant precise location permission when prompted.

## Important limitations
- This is a starter implementation, not a tested production release.
- Standard PDF maps are displayed, but GPS cannot be accurately drawn on top of an ordinary PDF unless it contains geospatial metadata or the map is calibrated using known control points. Calibration/geospatial PDF parsing is not included yet.
- Track and waypoint CSV files are saved under Android/data/com.forestgps.navigator/files/ForestGPS/.
- Test GPS accuracy and map suitability in a safe, known area before relying on it for official patrol/navigation.

## Build an APK using GitHub Actions from a phone
1. Create a free GitHub account and a new repository.
2. In the repository, upload the extracted project files and folders, including the hidden `.github/workflows/build-apk.yml` file. Keep the folder structure unchanged.
3. Open the repository's Actions tab.
4. Select "Build Forest GPS Navigator APK" and tap "Run workflow".
5. When the workflow completes successfully, open its run page, scroll to Artifacts, and download `ForestGPSNavigator-debug-apk`.
6. Extract the downloaded artifact ZIP; it contains `app-debug.apk`. Tap it to install and allow installation from that source if Android asks.

This workflow builds a debug APK. It is unsigned for Play Store release but can be installed manually for testing. Build success is not guaranteed until run; fix any reported Gradle/build errors before installation.
