<p align="center">
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest"><img src="https://img.shields.io/github/v/release/ENDDREAM-ZM/KeyBox-For-Android?color=orange&label=下载" alt="下载" /></a>
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases"><img src="https://shields.io/github/downloads/ENDDREAM-ZM/KeyBox-For-Android/total?label=下载量&color=yellow" alt="下载量" /></a>
</p>

# KeyBox

[English](README.md) | **中文**

## 简介

一个不联网的本地密码管理器

## 功能

- 条目：标题、用户名、密码、网址、应用包名、分类、标签、备注、收藏
- 自定义字段：可自定义字段，类型为文本、隐藏、开关，可加一键复制
- 搜索、筛选、多选、回收站
- 密码与用户名生成器
- 动态令牌，扫码或手动添加
- 自动填充服务与无障碍快速填充
- 指纹解锁、剪贴板定时清除、禁止截图

## 支持版本

Android 9 及以上，minSdk 28，targetSdk 37。

## 下载

[最新版本](https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest)

## 构建

```sh
./gradlew :app:assembleRelease
```

签名从 `keystore.properties` 读取，缺少该文件时产出未签名包。

## 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose)：界面
- [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix)：Miuix 材质
- [Backdrop](https://github.com/Kyant0/AndroidLiquidGlass)：液态玻璃
- [CameraX](https://developer.android.com/media/camera/camerax)：扫码

## 许可

[Apache-2.0](LICENSE)
