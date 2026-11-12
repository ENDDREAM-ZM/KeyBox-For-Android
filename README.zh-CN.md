<p align="center">
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases/latest"><img src="https://img.shields.io/github/v/release/ENDDREAM-ZM/KeyBox-For-Android?color=orange&label=下载" alt="下载" /></a>
  <a href="https://github.com/ENDDREAM-ZM/KeyBox-For-Android/releases"><img src="https://shields.io/github/downloads/ENDDREAM-ZM/KeyBox-For-Android/total?label=下载量&color=yellow" alt="下载量" /></a>
</p>

---

# KeyBox For Android

[English](README.md) | **中文**

不联网的本地密码管理器，所有数据只在设备里保存。

## 功能

- 条目：标题、用户名、密码、网址、包名、分类、标签、备注、收藏
- 搜索、分类筛选、多选
- 回收站，保留期可设
- 密码与用户名生成器
- 动态令牌，扫码或手动添加
- 自动填充服务与无障碍快速填充
- 指纹解锁、剪贴板定时清除、禁止截图
- 站点图标多源获取，同域名共用

## 安全

`vault.pmv` 始终加密。密钥由 PBKDF2-SHA256 迭代 60 万次派生，内容依次经过 AES-256-CTR、AES-256-GCM、ChaCha20-Poly1305，最外层再做 HMAC。

未设置主密码时用 Android Keystore 里的密钥加密，设置主密码后改用主密码。

## 构建

```sh
./gradlew :app:assembleRelease
```

签名从 `keystore.properties` 读取，没有这个文件则产出未签名包。

## 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose)：界面
- [Miuix](https://github.com/miuix-kotlin-multiplatform/miuix)：Miuix 皮肤
- [Haze](https://github.com/chrisbanes/haze)：顶栏底栏模糊
- [CameraX](https://developer.android.com/media/camera/camerax)：扫码

## 许可

[MIT](LICENSE)
