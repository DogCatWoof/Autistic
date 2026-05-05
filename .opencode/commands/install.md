---
description: Build APK, install on device, and launch the app
---
Run `./gradlew installDebug` to build the debug APK and install it on the connected Android device. Show any build errors if the install fails.

After a successful install, launch the app:
`adb shell am start -n org.meow.autistic/.MainActivity`
