# KeyBox For Android

[![Download](https://img.shields.io/github/v/release/ENDDREAM-ZM/KeyBox-For-Android?color=orange&label=Download)](https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest) [![Total](https://shields.io/github/downloads/ENDDREAM-ZM/KeyBox-For-Android/total?label=Counts&color=yellow)](https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases)

## Introduction

本地密码管理器，不联网。数据只在本机，网络权限只用来取一言和图标。

条目、分类、回收站、密码生成、动态令牌、自动填充、快速填充、指纹解锁。

## Supported Versions

Android 9 ~ 17

## Install

下载 APK 安装。

## Download

- [Github Releases](https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest)

## Security

`vault.pmv` 始终加密。PBKDF2-SHA256 迭代 60 万次派生密钥，AES-256-CTR、AES-256-GCM、ChaCha20-Poly1305 三层加密加外层 HMAC。没设主密码时用 Android Keystore 里的设备密钥，设了就用主密码。

## Build

```
./gradlew :app:assembleRelease
```

签名配置放 `keystore.properties`，没有这个文件就出未签名包。

## Credits

- [Jetpack Compose](https://developer.android.com/jetpack/compose): 界面
- [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix): Miuix 皮肤
- [Haze](https://github.com/chrisbanes/haze): 顶栏底栏模糊
- [CameraX](https://developer.android.com/media/camera/camerax): 扫码

## License

MIT
