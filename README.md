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
### Port forwarding

```bash
~/Library/Android/sdk/platform-tools/adb forward tcp:8080 tcp:8080
```

### Web dashboard

```bash
cd ~/Desktop/AndroidNetworkLogger/web-dashboard
npm run dev
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

## Tech stack

| Component | Technologies |
|-----------|-------------|
| Library | Kotlin, OkHttp, Room, Ktor (CIO), Coroutines |
| Demo app | Kotlin, Jetpack Compose, Retrofit, OkHttp |
| Dashboard | React, TypeScript, Vite, Tailwind CSS |
