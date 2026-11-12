# KeyBox For Android

本地密码管理器，不联网。数据只在本机，网络权限只用来取一言和图标。

## 功能

条目增删改查、搜索、分类、回收站、密码生成、动态令牌、自动填充、快速填充、指纹解锁。

## 加密

`vault.pmv` 始终加密。PBKDF2-SHA256 迭代 60 万次派生密钥，AES-256-CTR、AES-256-GCM、ChaCha20-Poly1305 三层加密加外层 HMAC。没设主密码时用 Android Keystore 里的设备密钥，设了就用主密码。

格式见 [SPEC.md](SPEC.md)。

## 构建

```
./gradlew :app:assembleRelease
```

签名配置放 `keystore.properties`，没有这个文件就出未签名包。

## 许可

MIT
