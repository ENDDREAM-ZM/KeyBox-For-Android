# KeyBox For Android

不联网的本地密码管理器，所有数据只在本机保存。网络权限仅用于获取一言和图标，与本地密码库无关。

- 平台：Android（minSdk 28 ～ targetSdk 37）
- 包名：`com.enddream.keybox`
- 界面：Jetpack Compose，两套皮肤 Material 3 / Miuix
- 语言：Kotlin + Java

## 功能

- 条目：标题、用户名、密码、网址、应用包名、分类、标签、备注、收藏
- 搜索、分类筛选、多选批量操作（收藏 / 移入回收站 / 彻底删除）
- 回收站：保留期可设，支持恢复与彻底删除
- 生成器：随机密码（4–256 位）、随机用户名（3–20 位）
- 双重认证：扫描二维码或手动添加，SHA1 / 6 位 / 30 秒
- 自动填充：系统 Autofill 服务 + 无障碍快速填充
- 安全：生物识别解锁、剪贴板定时清除、禁止截图开关
- 图标：站点图标（多源并发）→ 应用图标 → 内置占位，同域名共用

## 加密

密码库 `vault.pmv` 始终加密存储：

```
PBKDF2-HMAC-SHA256（600000 次）→ 160 字节密钥
  ├ AES-256-CTR + HMAC-SHA256
  ├ AES-256-GCM
  ├ ChaCha20-Poly1305
  └ 外层 HMAC-SHA256
```

未设置主密码时使用 Android Keystore 中的设备密钥加密；设置主密码后改用主密码加密。
格式细节见 [SPEC.md](SPEC.md)。

## 构建

```bash
cd android_gradle
./gradlew :app:assembleRelease
```

签名配置放在 `android_gradle/keystore.properties`（不入库）：

```properties
storeFile=keybox.jks
storePassword=******
keyAlias=KeyBox
keyPassword=******
```

缺少该文件时会产出未签名包。

## 许可

[MIT](LICENSE)
