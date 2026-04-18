# SKTC Ticket Printer App

Android ticketing app for bus operations with route-based fare calculation, Bluetooth thermal printing, manager-controlled setup, and daily reporting.

This README is a complete feature + implementation map of what is currently available in this codebase.

## Current Build Info

- App version: `2.2.0`
- Version code: `8`
- Compile SDK: `34`
- Min SDK: `24`
- Target SDK: `34`
- Kotlin/JVM target: `17`
- Main Android package: `com.sktc.ticketprinter`

Source: [app/build.gradle.kts](app/build.gradle.kts), [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml)

## High-Level Architecture

### User Flow

1. Splash screen -> Home
2. Home routes user to:
   - Manager Setup
   - Commute Ticket (modern)
   - Daily Report
   - Settings

### Core Modules

- Navigation and startup: [app/src/main/java/com/ksrtc/ticketprinter/SplashActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/SplashActivity.kt), [app/src/main/java/com/ksrtc/ticketprinter/HomeActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/HomeActivity.kt)
- Ticket issuance and fare engine: [app/src/main/java/com/ksrtc/ticketprinter/CommuteTicketActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/CommuteTicketActivity.kt)
- Manager configuration and auth: [app/src/main/java/com/ksrtc/ticketprinter/ManagerSetupActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/ManagerSetupActivity.kt)
- Reports and history: [app/src/main/java/com/ksrtc/ticketprinter/DailyReportActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/DailyReportActivity.kt)
- Route parser and fare matrix: [app/src/main/java/com/ksrtc/ticketprinter/RouteDslParser.kt](app/src/main/java/com/ksrtc/ticketprinter/RouteDslParser.kt)
- Bluetooth print transport: [app/src/main/java/com/ksrtc/ticketprinter/BluetoothPrinterManager.kt](app/src/main/java/com/ksrtc/ticketprinter/BluetoothPrinterManager.kt)
- Ticket bitmap rendering: [app/src/main/java/com/ksrtc/ticketprinter/TicketFormatter.kt](app/src/main/java/com/ksrtc/ticketprinter/TicketFormatter.kt)
- Theme management: [app/src/main/java/com/ksrtc/ticketprinter/AppThemeManager.kt](app/src/main/java/com/ksrtc/ticketprinter/AppThemeManager.kt)

## Features Available and How They Are Built

## 1) Splash + Home Navigation

### What you get

- 1-second splash screen with app version display
- Home dashboard with quick navigation buttons

### How it is built

- Splash delay and version label in [app/src/main/java/com/ksrtc/ticketprinter/SplashActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/SplashActivity.kt)
- Home button navigation via explicit intents in [app/src/main/java/com/ksrtc/ticketprinter/HomeActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/HomeActivity.kt)

## 2) Manager Setup (Zone -> Division -> Route)

### What you get

- Zone and division selection
- Route selection from route DSL
- Auto-populated route directions and bus type
- Bus numbers storage
- Confirm-before-print toggle
- Admin auth using pass key or biometric

### How it is built

- Large zone-to-division map and spinner chain in [app/src/main/java/com/ksrtc/ticketprinter/ManagerSetupActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/ManagerSetupActivity.kt)
- Biometric support via `BiometricManager` and `BiometricPrompt`
- Pass key verification uses manager key in code (`1415`)
- Configuration persisted in SharedPreferences `manager_setup`

Stored keys include:

- `zone`, `division`, `route`, `route_number`, `bus_type`, `bus_numbers`
- `confirm_printing`
- `auth_method`

## 3) Route Parsing and Fare Matrix Engine

### What you get

- Dynamic route loading from asset file
- Up and down route generation
- Stop-to-stop fare lookup from matrix
- Bus type matching for filtering

### How it is built

- Route DSL parser in [app/src/main/java/com/ksrtc/ticketprinter/RouteDslParser.kt](app/src/main/java/com/ksrtc/ticketprinter/RouteDslParser.kt)
- Data source file: [app/src/main/assets/routes](app/src/main/assets/routes)
- Fare lookup method: `ParsedRoute.fareBetween(fromStop, toStop)`
- Reverse route matrix generation: `buildReverseFares(...)`

## 4) Commute Ticketing (Modern Screen)

### What you get

- From/To stop selection
- Adult/child/luggage counters
- Pass type selection
- Live fare calculation
- Print, reset, report, and day-close actions
- Ticket and revenue tracking

### How it is built

- Main implementation in [app/src/main/java/com/ksrtc/ticketprinter/CommuteTicketActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/CommuteTicketActivity.kt)
- Route and stop data loaded from `manager_setup` + `RouteDslParser`
- SharedPreferences `daily_report` stores totals and history

### Important business rules currently enforced

- Max passengers per ticket:
  - Adults: 5
  - Children: 5
- Day Pass restrictions:
  - Allowed only on city class services (with AC-specific message)
- Pass and luggage restrictions:
  - Passes are not allowed on luggage-only tickets
  - Passenger and luggage journeys must be printed as separate tickets
- Luggage handling:
  - Manual luggage weight input dialog available
  - Luggage fare is computed with per-kg rule based on segment fare

### Fare calculation mechanics

Built using helper methods in `CommuteTicketActivity`:

- `getBusTypeFareMultiplier(...)`
- `getDayPassAmount()`
- `getLuggageFarePerKg(...)`
- `getChildRateFactor()`

Data comes from:

- Route fare matrix in parsed route
- Settings preferences like `child_ticket_rate`

## 5) Day Close and Trip Snapshots

### What you get

- Day-close flow that captures a route snapshot
- Trip reports list for historical closure entries

### How it is built

- Day-close trigger from `btnDayClose`
- Snapshot persistence via `saveTripSnapshot()`
- Stored as JSON array in `daily_report` key `trip_reports`
- Ticket history stored in `daily_report` key `ticket_history`

Source: [app/src/main/java/com/ksrtc/ticketprinter/CommuteTicketActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/CommuteTicketActivity.kt)

## 6) Daily Report Screen

### What you get

- Current-day totals (tickets, adults, children, passes, luggage, revenue)
- Closed report cards with print/delete actions
- Ticket history section

### How it is built

- Screen and logic in [app/src/main/java/com/ksrtc/ticketprinter/DailyReportActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/DailyReportActivity.kt)
- Data read from SharedPreferences:
  - `manager_setup`
  - `daily_report`
- Report print text generated in `buildReportPrintText(...)`

## 7) Ticket Rendering and Printing

### What you get

- Thermal-print friendly ticket output
- Structured sections (header, route row, counts, totals, footer)
- Senior ticket variant rendering

### How it is built

- Bitmap ticket rendering in [app/src/main/java/com/ksrtc/ticketprinter/TicketFormatter.kt](app/src/main/java/com/ksrtc/ticketprinter/TicketFormatter.kt)
- Text width fit logic with `fitTextSize(...)` and optional compression `removeVowelsForFit(...)`
- Bluetooth transport in [app/src/main/java/com/ksrtc/ticketprinter/BluetoothPrinterManager.kt](app/src/main/java/com/ksrtc/ticketprinter/BluetoothPrinterManager.kt)
- Uses RFCOMM SPP UUID: `00001101-0000-1000-8000-00805F9B34FB`

Required permissions are declared in [app/src/main/AndroidManifest.xml](app/src/main/AndroidManifest.xml).

## 8) Settings and Feature Toggles

### What you get

- Print behavior settings
- Ticketing behavior settings
- Theme and general toggles

### How it is built

- Preferences UI in [app/src/main/res/xml/preferences.xml](app/src/main/res/xml/preferences.xml)
- Settings screen in [app/src/main/java/com/ksrtc/ticketprinter/SettingsActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/SettingsActivity.kt)
- Theme mode applied via [app/src/main/java/com/ksrtc/ticketprinter/AppThemeManager.kt](app/src/main/java/com/ksrtc/ticketprinter/AppThemeManager.kt)

Key toggles include:

- `confirm_printing`, `print_time_expense`, `print_stops_pass`, `printer_width`
- `stage_lock`, `group_tickets`, `child_tickets`, `child_ticket_rate`, `enable_passes`, `luggage_tickets`
- `theme_mode`, `ignore_bluetooth_error`, `beep_on_error`

## 9) Legacy Ticket Screen

### What you get

- Basic legacy commute ticket UI still available

### How it is built

- [app/src/main/java/com/ksrtc/ticketprinter/MainActivity.kt](app/src/main/java/com/ksrtc/ticketprinter/MainActivity.kt)
- Registered as legacy activity in manifest label

This exists alongside the modern ticketing screen and is useful for compatibility/testing.

## 10) Localization (English + Kannada)

### What you get

- English strings (default)
- Kannada translations file

### How it is built

- Default resources: [app/src/main/res/values/strings.xml](app/src/main/res/values/strings.xml)
- Kannada resources: [app/src/main/res/values-kn/strings.xml](app/src/main/res/values-kn/strings.xml)

### Current status

- Bilingual resource files are implemented and active
- Major screens are migrated to string resources
- A small number of titles/messages still exist as hardcoded literals in code and manifest labels; these can be fully migrated in a follow-up cleanup

## Data and Storage Model

## SharedPreferences

- `manager_setup`: Admin configuration for route and bus context
- `daily_report`: Runtime counters, revenue, ticket history, trip snapshots

## Route and Fare Data

- Primary runtime route source: `app/src/main/assets/routes`
- Parsed into in-memory route objects via `RouteDslParser`

## SQLite Helper (present in codebase)

- [app/src/main/java/com/ksrtc/ticketprinter/RouteDatabaseHelper.kt](app/src/main/java/com/ksrtc/ticketprinter/RouteDatabaseHelper.kt)
- Contains tables `Stops` and `Stages` and a parser for route lines
- Useful for local fallback/alternate data handling patterns

## Non-Android Preview Utilities (Root)

- Python desktop preview launcher:
  - [Click_To_Run_Ticket_App.bat](Click_To_Run_Ticket_App.bat)
  - [desktop_app.py](desktop_app.py)
- Kotlin Swing desktop preview:
  - [DesktopApp.kt](DesktopApp.kt)
- HTML mock UI preview:
  - [preview.html](preview.html)

These are convenience/testing utilities and are separate from the Android APK runtime path.

## Build and Run

## Android (Recommended)

1. Open the project in Android Studio.
2. Let Gradle sync complete.
3. Run the `app` module on device/emulator.

## Command-line note

- This repository currently does not include `gradlew` / `gradlew.bat` scripts at root.
- Use Android Studio sync/build, or generate Gradle wrapper scripts before terminal-only builds.

## License

Proprietary. All rights reserved.

See [LICENSE](LICENSE) for legal terms.
