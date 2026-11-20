<p align="center">
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest"><img src="https://img.shields.io/github/v/release/ENDDREAM-ZM/KeyBox-For-Android?color=orange&label=download" alt="Download" /></a>
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases"><img src="https://shields.io/github/downloads/ENDDREAM-ZM/KeyBox-For-Android/total?label=downloads&color=yellow" alt="Downloads" /></a>
</p>

# KeyBox

**English** | [中文](README.zh-CN.md)

## Introduction

An offline local password manager

## Features

- Entries: title, username, password, URL, app package name, category, tags, note, favorite
- Custom fields: name your own fields, type text, hidden or switch, with a tap-to-copy button
- Search, filter, multi-select, trash
- Password and username generator
- TOTP, added by QR scan or manually
- Autofill service and accessibility quick fill
- Biometric unlock, clipboard timed clear, screenshot blocking

## Supported Versions

Android 9 and above, minSdk 28, targetSdk 37.

## Download

[Latest release](https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest)

## Build

```sh
./gradlew :app:assembleRelease
```

Signing is read from `keystore.properties`; without that file the build produces an unsigned APK.

## Credits

- [Jetpack Compose](https://developer.android.com/jetpack/compose): UI
- [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix): Miuix material
- [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass): liquid glass
- [CameraX](https://developer.android.com/media/camera/camerax): QR scanning

## License

[Apache-2.0](LICENSE)
