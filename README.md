# Android Network Logger

A project toolkit for inspecting HTTP traffic from an Android app in real time. The project has three parts:

- **network-logger-lib** — Android library that intercepts OkHttp requests, stores them in Room, and serves them over an embedded Ktor server.
- **app** — Demo Android app with buttons that trigger sample API calls.
- **web-dashboard** — React dashboard that connects to the phone via ADB port forwarding.

---

## Prerequisites

Install these before running the project:

- [Android Studio](https://developer.android.com/studio) (includes the Android SDK and emulator)
- [Node.js](https://nodejs.org/) (v18 or newer recommended)
- An Android emulator or a physical device with USB debugging enabled

---

## Project structure

```
AndroidNetworkLogger/
├── app/                    # Demo Android application
├── network-logger-lib/     # Network inspection library
├── web-dashboard/          # React + TypeScript dashboard
├── settings.gradle.kts
└── README.md
```

---

## ADB path (macOS)

`adb` is not always available in your terminal PATH. On a default Android Studio install it lives here:

```
~/Library/Android/sdk/platform-tools/adb
```

Full expanded path:

```
/Users/<your-username>/Library/Android/sdk/platform-tools/adb
```

### Optional: add ADB to your PATH

Add this line to `~/.zshrc`, then restart the terminal:

```bash
export PATH="$HOME/Library/Android/sdk/platform-tools:$PATH"
```

After that you can type `adb` directly instead of the full path.

---

## How to run everything

You need three things running at the same time: the emulator, the Android app, and the web dashboard. ADB port forwarding bridges the dashboard on your Mac to the server inside the app.

### Run checklist

Use this list every time you work on the project:

- [ ] **1. Open the project** in Android Studio (`AndroidNetworkLogger` folder)
- [ ] **2. Start an emulator** (Device Manager) or connect a physical device
- [ ] **3. Verify the device is detected**
  ```bash
  ~/Library/Android/sdk/platform-tools/adb devices
  ```
  You should see a line ending in `device`.
- [ ] **4. Run the Android app** from Android Studio (select the `app` run configuration, then Run)
- [ ] **5. Set up port forwarding** (required after every emulator restart)
  ```bash
  ~/Library/Android/sdk/platform-tools/adb forward tcp:8080 tcp:8080
  ```
- [ ] **6. Confirm the server is reachable**
  ```bash
  curl http://localhost:8080/api/logs
  ```
  A JSON array (even `[]`) means forwarding works.
- [ ] **7. Install dashboard dependencies** (first time only)
  ```bash
  cd web-dashboard
  npm install
  ```
- [ ] **8. Start the web dashboard**
  ```bash
  cd web-dashboard
  npm run dev
  ```
  This runs port forwarding automatically, then starts Vite.
- [ ] **9. Open the dashboard** in your browser at `http://localhost:5173`
- [ ] **10. Tap a button** in the demo app (e.g. "GET – Success") and confirm the request appears in the dashboard

---

## Terminal reference

All commands assume the project is at `~/Desktop/AndroidNetworkLogger`.

### Android app (alternative to Android Studio)

```bash
cd ~/Desktop/AndroidNetworkLogger
./gradlew :app:installDebug
```

Then open **Network Logger Demo** on the device.

### Port forwarding

```bash
~/Library/Android/sdk/platform-tools/adb forward tcp:8080 tcp:8080
```

### Web dashboard

```bash
cd ~/Desktop/AndroidNetworkLogger/web-dashboard
npm run dev
```

To start Vite without running `adb forward` (if you already forwarded manually):

```bash
npm run dev:only
```

---

## What each component does

### Library (`network-logger-lib`)

1. `WebNetworkLoggerInterceptor` captures OkHttp request/response data.
2. Logs are saved to a Room database on the device.
3. An embedded Ktor server exposes:
   - `GET http://localhost:8080/api/logs` — historical logs
   - `WS  ws://localhost:8080/ws/logs` — real-time log stream

### Demo app (`app`)

- Initializes the library in `DemoApplication`.
- Registers the interceptor on a shared OkHttp client.
- Provides three buttons that call JSONPlaceholder APIs (success, 404, POST).

### Web dashboard (`web-dashboard`)

- Fetches historical logs on load.
- Connects to the WebSocket for live updates.
- Split-screen UI: request list on the left, details on the right.

---

## Troubleshooting

### Dashboard shows a connection error

1. Make sure the demo app is open on the emulator (not just installed).
2. Run port forwarding again:
   ```bash
   ~/Library/Android/sdk/platform-tools/adb forward tcp:8080 tcp:8080
   ```
3. Test with curl:
   ```bash
   curl http://localhost:8080/api/logs
   ```
4. Click **Retry** in the dashboard header.

### `adb: command not found`

Use the full path:

```bash
~/Library/Android/sdk/platform-tools/adb forward tcp:8080 tcp:8080
```

### Android Studio cannot find the `app` module

1. **File → Sync Project with Gradle Files**
2. Select the **`app`** run configuration in the toolbar dropdown
3. If needed: **Run → Edit Configurations** → set Module to `network-logger-lib.app`

### `Activity class does not exist`

Uninstall old app versions, then reinstall:

```bash
~/Library/Android/sdk/platform-tools/adb uninstall com.example.network_logger_lib
~/Library/Android/sdk/platform-tools/adb uninstall com.example.demoapp
```

Rebuild and run from Android Studio.

### No requests appear after tapping a button

- Confirm the app has internet access (emulator usually does by default).
- Check Logcat in Android Studio for errors.
- Verify `NetworkLogger.init()` is called in `DemoApplication.onCreate()`.

---

## Build commands

```bash
# Build the Android app
./gradlew :app:assembleDebug

# Build the web dashboard
cd web-dashboard && npm run build
```

---

## Tech stack

| Component | Technologies |
|-----------|-------------|
| Library | Kotlin, OkHttp, Room, Ktor (CIO), Coroutines |
| Demo app | Kotlin, Jetpack Compose, Retrofit, OkHttp |
| Dashboard | React, TypeScript, Vite, Tailwind CSS |
