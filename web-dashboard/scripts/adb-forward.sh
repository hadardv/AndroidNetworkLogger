#!/usr/bin/env sh
# Forwards host port 8080 to the Android device so the web dashboard can reach the embedded server.
ADB="${ANDROID_HOME:-$HOME/Library/Android/sdk}/platform-tools/adb"

if [ ! -x "$ADB" ]; then
  echo "adb not found at $ADB"
  echo "Install Android SDK platform-tools or set ANDROID_HOME."
  exit 1
fi

"$ADB" forward tcp:8080 tcp:8080
echo "Port forwarding active: localhost:8080 -> device:8080"
