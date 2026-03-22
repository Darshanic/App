# SKTC Ticket Printer App

This repository contains a full Android mobile application and testing suite designed to emulate the physical ticketing machines used by the KSRTC, specifically tailored for Bluetooth thermal receipt printing and dynamic ticket calculation.

It was built entirely in Kotlin and includes a custom SQLite database engine to calculate fares directly on the device using user-defined route text files.

## Project Structure

The project directory (`e:\App`) is organized into several key components:

### 1. The Core Android Application

The `app/src/main/` folder holds the true Android application.

- `MainActivity.kt`: The main screen. Allows selecting source, destination, adults, and child passengers to dynamically price trips.
- `BluetoothPrinterManager.kt`: The hardware integration layer. Manages Bluetooth connection/permissions (`Android 12+`) and sets up an RFCOMM socket with standard ESC/POS portable receipt printers over UUID `00001101-0000-1000-8000-00805F9B34FB`.
- `TicketFormatter.kt`: Because standard ESC/POS printers do not support Kannada fonts natively, this class uses Android Canvas to accurately render the ticket with standard Kannada text, corporation headers, layouts, and logos into a continuous `Bitmap`. It converts the image into ESC/POS binary raster commands to flawlessly print accurate Kannada layouts.
- `RouteDatabaseHelper.kt`: A dynamic SQLite Database. Upon initialization, it attempts to read the `english_routes.txt` file from the local file system. It automatically creates local tables for dynamic destinations and tracks exactly what the fare must be as configured by your text file.
- `SettingsActivity.kt`: A comprehensive configuration drawer that allows toggling printer character width, enabling/disabling child ticket rate (e.g., 50% discount), luggage ticket fares, and "Confirm Before Printing" alerts.

### 2. Custom Routings configuration

- **`english_routes.txt`**: This is your master data file. The application reads it row-by-row.
  Just define your stops separated by a comma. The application understands it automatically:
  ```
  Route, Mandya, Srirangapatna, , ORDINARY
  Stop, Mandya, 6, 8, 10, 12...    // Literals corresponding to exact fare multipliers or jumps.
  Stop, Kallahalli, 6, 8, 10...
  ```

### 3. Desktop Testing Applications

Because developing and deploying an Android APK can be cumbersome, two separate non-Android runnable previews were included in the root directory for instantaneous testing right on a Windows computer:

- `Click_To_Run_Ticket_App.bat / desktop_app.py`: A fully functional desktop native window using Python / Tkinter. It directly links to the `english_routes.txt` file and functions exactly like the Android App would—allowing you to test if your numeric fares and routes are configured properly.
- `preview.html`: A static Dark-Mode Web simulation of the XML layout that can be double-clicked and opened in Google Chrome.

---

## How to Build & Run

**To verify the ticket calculations instantly on Windows:**

1. Navigate to your `e:\App` directory.
2. Double-click `Click_To_Run_Ticket_App.bat`. This will pop open the interactive Ticket Application on your desktop!

**To build the Android App for your Physical Phone/Ticketing Machine:**

1. Download and install **Android Studio**.
2. Go to **File > Open**, and navigate to `E:\App`.
3. Let the Gradle syncing background processes complete (may take 2 minutes on first run).
4. From the top status bar, go to **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
5. Transfer the generated `.apk` file to your mobile device or Bluetooth printer hardware and tap to install it.
6. Ensure Bluetooth is turned on, pair your printer in the Android OS, and launch the app!

---

## Setup Progress (March 2026)

The following setup/troubleshooting steps are already completed for this project:

- Android SDK path is configured to `E:\SDK`.
- Required SDK components were installed (`platforms`, `platform-tools`, `build-tools`, `cmdline-tools`, `emulator`).
- Project `local.properties` was added with:

  ```properties
  sdk.dir=E:\\SDK
  ```

- Root Gradle configuration was migrated to modern plugin DSL in `build.gradle.kts`:

  ```kotlin
  plugins {
      id("com.android.application") version "8.1.0" apply false
      id("org.jetbrains.kotlin.android") version "1.9.0" apply false
  }
  ```

- Gradle user cache was cleared once to resolve possible corrupted dependency downloads.

## What To Do Next

1. Open this project in Android Studio (`E:\App`).
2. Wait for automatic Gradle import/sync to finish.
3. If sync fails with dependency/plugin errors:
   - Click the Gradle/Sync error suggestion to re-download dependencies.
   - Use `File > Invalidate Caches` only if repeated syncs fail.
   - Close Android Studio and stop Java/Gradle daemons, then reopen.
4. Create/start a device from Device Manager (or connect a physical Android phone with USB debugging enabled).
5. Run the `app` configuration.

## Important Note

This repository currently contains `gradle/wrapper/gradle-wrapper.properties`, but does not include `gradlew` and `gradlew.bat` scripts at the root.

- Android Studio can still import/sync using its own Gradle integration.
- For terminal builds (`.\gradlew ...`), add/generate wrapper scripts first.
