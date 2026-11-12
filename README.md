<p align="center">
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest"><img src="https://img.shields.io/github/v/release/ENDDREAM-ZM/KeyBox-For-Android?color=orange&label=download" alt="Download" /></a>
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases"><img src="https://shields.io/github/downloads/ENDDREAM-ZM/KeyBox-For-Android/total?label=downloads&color=yellow" alt="Downloads" /></a>
</p>

---

# KeyBox For Android

**English** | [中文](README.zh-CN.md)

An offline password manager for Android. All data stays on your device.

## Features

- Entries: title, username, password, URL, package name, category, tags, note, favorite
- Search, category filter, multi-select
- Trash with configurable retention
- Password and username generator
- TOTP, added by QR scan or manually
- Autofill service and accessibility quick fill
- Biometric unlock, clipboard auto-clear, screenshot blocking
- Site icons fetched from multiple sources, shared per domain

## Security

`vault.pmv` is always encrypted. The key is derived with PBKDF2-SHA256 at 600000 iterations, then the payload goes through AES-256-CTR, AES-256-GCM and ChaCha20-Poly1305, with an outer HMAC.

Without a master password the vault is encrypted with a key held in the Android Keystore. Setting a master password switches the key to the password.

## Build

```sh
./gradlew :app:assembleRelease
```

Signing is read from `keystore.properties`. Without that file the build produces an unsigned APK.

## Credits

- [Jetpack Compose](https://developer.android.com/jetpack/compose): UI
- [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix): Miuix skin
- [Haze](https://github.com/chrisbanes/haze): top and bottom bar blur
- [CameraX](https://developer.android.com/media/camera/camerax): QR scanning

## License

[MIT](LICENSE)
