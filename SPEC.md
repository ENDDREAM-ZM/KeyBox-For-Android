# KeyBox 数据格式规范 v4

本文件是 Windows 版（KeyBox.exe，C 实现）与 Android 版（KeyBox.apk，Java 实现）之间唯一的互通契约。
两端各自独立实现同一份规范，因此同一个 `vault.pmv` / 导出文件可以在两端互相打开。

---

## 1. 主库文件 `vault.pmv`

```
+--------------------------------------------------+
| magic        6 字节   "KEYBOX"                    |
+--------------------------------------------------+
| hdrLen       4 字节   大端 uint32                 |
+--------------------------------------------------+
| header       hdrLen 字节  UTF-8 JSON（明文）      |
+--------------------------------------------------+
| body         AES-256-GCM 密文 || tag(16)          |
+--------------------------------------------------+
```

* magic：固定 `KEYBOX`（6 字节）。不兼容历史格式，读不到 magic 即报"不是 KeyBox 保险库"。
* header（v4，当前写入格式）：
  ```json
  {"ver":4,"kdf":"PBKDF2-HMAC-SHA256","iter":600000,
   "layers":"aesctr+hmac+aesgcm+chacha20poly1305+hmac",
   "salt":"<b64 16B>","nonce":"<b64 16B>","nonce2":"<b64 12B>","nonce3":"<b64 12B>"}
  ```
* v4 密钥：PBKDF2-HMAC-SHA256(主密码, salt, iter) → **160 字节**，五把密钥：
  `kCtr[0..31]`、`kAes[32..63]`、`kCha[64..95]`、`kMac1[96..127]`、`kMac[128..159]`。
* v4 加密流水线（四层，每层算法不同）：
  1. `L1 = AES-256-CTR(kCtr, nonce) ‖ HMAC-SHA256(kMac1, AES-CTR 密文)`（流加密 + 独立 MAC）
  2. `L2 = AES-256-GCM(kAes, nonce2, AAD, L1)`
  3. `L3 = ChaCha20-Poly1305(kCha, nonce3, AAD, L2)`
  4. `MAC = HMAC-SHA256(kMac, AAD ‖ L3)`
* 读取兼容 v3（`layers` 含 `whiten`）：旧格式按原三层流程解开，保存时自动升级为 v4。
* 默认迭代次数 600000（v3 文件保留各自头里的 iter）。
* v3 密钥：PBKDF2-HMAC-SHA256(主密码 UTF-8, salt, iter=300000) → 128 字节，切为四把独立密钥：
  `kWhite[0..31]`、`kAes[32..63]`、`kCha[64..95]`、`kMac[96..127]`（仅用于读取旧文件）。
* 加密流水线（四层）：
  1. 白化：`W = P XOR KS(kWhite, nonce)`，KS 由 HMAC-SHA256(kWhite, nonce‖ctr_be32‖"KBX\x01") 逐块生成（自研）
  2. `C1 = AES-256-GCM(kAes, nonce2, AAD, W)`，输出 ct1‖tag16
  3. `C2 = ChaCha20-Poly1305(kCha, nonce2, AAD, C1)`，输出 ct2‖tag16（RFC 8439）
  4. `MAC = HMAC-SHA256(kMac, AAD ‖ C2)`
* body = `C2 ‖ MAC(32)`；解密顺序相反，先验 MAC 再逐层解。
* AAD = `magic || hdrLen || header`（改头部任何字节都会解密失败）。
* 每次保存都重新生成 salt 与 nonce。

## 1b. 明文主库（未设置主密码时，Android 端使用）

```
magic "KEYBOXP1" (8) | hdrLen (4,BE) | {"ver":2,"enc":"none","app":"KeyBox"} | 明文 payload
```

* 仅当用户**主动不设置主密码**时，Android 端才用这种容器保存；界面会常驻提醒。
* Windows 端可直接打开 / 导入该文件（会提示该库未加密）。
* 用户随时可在「设置 → 主密码」里补设主密码，届时整库转为加密容器 `KEYBOX`。

## 2. 明文负载（payload）

```json
{
  "app": "KeyBox",
  "ver": 2,
  "updated": 1794000000000,
  "categories": [
    {"id": "a1b2c3d4e5f60718", "name": "工作", "order": 0}
  ],
  "entries": [
    {
      "id": "00112233445566aa",
      "title": "GitHub",
      "user": "alice",
      "pass": "S3cret!",
      "url": "https://github.com",
      "note": "备注",
      "tags": "开发,常用",
      "cat": "a1b2c3d4e5f60718",
      "fav": 0,
      "ctime": 1794000000000,
      "utime": 1794000000000,
      "del": 0,
      "dtime": 0
    }
  ]
}
```

字段说明：

| 字段 | 含义 |
|---|---|
| `id` | 16 位小写十六进制（8 字节随机），跨端识别同一条 |
| `cat` | 分类 id；空串 = 未分类 |
| `fav` | 0/1，收藏（列表置顶） |
| `del` / `dtime` | 0/1 与删除时间；`del=1` 表示在回收站，超过保留天数自动清理 |
| `ctime` / `utime` | 创建 / 最后修改（Unix 毫秒） |

解析器需容忍 v1 负载（无 `categories`、无 `cat/fav/del/dtime`），缺失字段取默认值。

## 3. 导出 / 导入

三种格式 × 加密/不加密 = 六种组合，两端都支持导出**和**导入。
界面上是「格式」下拉 + 「加密」复选框两个控件。

| 格式 | 不加密 | 加密 |
|---|---|---|
| KeyBox 原生（本软件专用，含分类/收藏/回收站等全部信息） | `.keybox` | `.keybox.enc` |
| JSON（通用，可读） | `.json` | `.json.enc` |
| CSV（通用，可进 Excel） | `.csv` | `.csv.enc` |

### 3.1 明文 JSON

```json
{
  "app": "KeyBox",
  "type": "keybox-export",
  "version": 1,
  "encrypted": false,
  "exported": 1794000000000,
  "categories": ["工作", "个人"],
  "entries": [
    {
      "id": "00112233445566aa",
      "title": "GitHub",
      "username": "alice",
      "password": "S3cret!",
      "url": "https://github.com",
      "note": "备注",
      "tags": "开发,常用",
      "category": "工作",
      "favorite": 0,
      "created": 1794000000000,
      "updated": 1794000000000
    }
  ]
}
```

### 3.2 明文 CSV

RFC 4180：字段含逗号、双引号、换行时用 `"` 包裹，内部 `"` 写成 `""`。UTF-8 编码，首行表头：

```
title,username,password,url,note,category,tags,favorite,created,updated,id
```

### 3.3 加密导出容器 `.json.enc` / `.csv.enc`

```
magic "KBOXEXP1" (8) | hdrLen (4,BE) | header JSON | GCM(明文 JSON 或 CSV 文本) || tag(16)
```

header：

```json
{"ver":1,"kind":"json","kdf":"PBKDF2-HMAC-SHA256","iter":200000,"salt":"<b64>","nonce":"<b64>"}
```

* `kind` 为 `keybox` / `json` / `csv`，决定解密后的内容类型。
* 密钥来自**导出密码**（可与主密码不同），其余参数与主库一致，AAD 同样覆盖整个头部。
* 用主库 magic 与导出 magic 区分，导入时据此决定用"主密码"还是"导出密码"。

### 3.4 导入规则

* 自动识别：`KEYBOX` → 加密主库（用主密码）；`KEYBOXP1` → 明文主库（无需密码）；
  `KBOXEXP1` → 加密导出（用导出密码）；
  首字符 `{` → 明文 JSON；否则按 CSV 解析。
* 用户每次导入时选择 **合并** 或 **替换**。
* 合并规则：
  * 分类按名字合并（同名复用）。
  * 条目有 `id` 且本地存在同 `id` → 取 `utime` 更新的一方；否则按 `(title, username)` 匹配，匹配不上则新增。
* CSV 表头兼容常见别名：`name/title/标题`、`login/username/user/用户名`、`password/pass/密码`、`url/website/网址`、`note/notes/备注`、`category/folder/分类`、`tags/标签`、`favorite/收藏`。

## 4. 本机设置（不进库、不参与互通）

迭代次数、自动锁定时间（5/15/30 分钟，默认 5）、在线获取图标开关（默认开）、
回收站保留天数（7/14/30，默认 7）、用户名生成器偏好、生物识别开关（仅 Android）。

## 5. 自研说明

SHA-256 / HMAC / PBKDF2 / AES-256 / GHASH / GCM、base64、JSON 解析与序列化、CSV 读写、
保险库与导出容器、密码与用户名生成器、强度评估、界面与交互全部为本项目从零实现。
Java 端仅调用平台自带 `javax.crypto`（AES-GCM）与 `javax.crypto.Mac`（HMAC）。
