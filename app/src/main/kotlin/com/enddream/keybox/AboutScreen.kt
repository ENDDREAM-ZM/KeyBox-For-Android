package com.enddream.keybox

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private data class Lib(val name: String, val version: String, val license: String, val note: String, val url: String)

private val platform = listOf(
    Lib("Android Keystore", "系统", "平台", "主密码与验证码密钥的硬件级保护", "https://developer.android.com/privacy-and-security/keystore"),
    Lib("BiometricPrompt", "系统", "平台", "指纹解锁", "https://developer.android.com/identity/sign-in/biometric-auth"),
    Lib("javax.crypto", "系统", "平台", "AES-GCM、ChaCha20-Poly1305、HMAC-SHA256、PBKDF2", "https://developer.android.com/reference/javax/crypto/package-summary"),
    Lib("HmacSHA1", "系统", "平台", "动态验证码计算", "https://developer.android.com/reference/javax/crypto/Mac"),
    Lib("Storage Access Framework", "系统", "平台", "导入导出时选择文件位置", "https://developer.android.com/guide/topics/providers/document-provider"),
    Lib("AccessibilityService", "系统", "平台", "快速填充时识别登录界面", "https://developer.android.com/guide/topics/ui/accessibility/service"),
    Lib("Autofill Framework", "系统", "平台", "系统级自动填充服务", "https://developer.android.com/guide/topics/text/autofill")
)

private val libs = listOf(
    Lib("Jetpack Compose", "1.12", "Apache-2.0", "全部界面与动画", "https://developer.android.com/jetpack/compose"),
    Lib("Miuix", "0.9.4", "Apache-2.0", "Miuix 风格的卡片、开关、输入框、导航与弹窗", "https://github.com/miuix-kotlin-multiplatform/miuix"),
    Lib("Haze", "1.7.3", "Apache-2.0", "顶栏和底栏的毛玻璃", "https://github.com/chrisbanes/haze"),
    Lib("AndroidX Activity", "1.13.0", "Apache-2.0", "界面宿主与文件选择回调", "https://developer.android.com/jetpack/androidx/releases/activity"),
    Lib("Kotlin Coroutines", "1.10", "Apache-2.0", "解密、导入导出放到后台线程", "https://github.com/Kotlin/kotlinx.coroutines"),
    Lib("Material Icons", "扩展包", "Apache-2.0", "界面图标", "https://fonts.google.com/icons"),
    Lib("CameraX", "1.4.1", "Apache-2.0", "扫码页的相机预览与帧分析", "https://developer.android.com/media/camera/camerax")
)

@Composable
fun AboutScreen(onClose: () -> Unit) {
    val ctx = LocalContext.current

    fun open(url: String) {
        try {
            ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (t: Throwable) {
        }
    }

    KScaffold(
        topBar = { KTopBar(title = "关于", onBack = onClose) },
        bottomBar = {},
        fab = {}
    ) { _ ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp)
                .padding(top = LocalTopInset.current + 4.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            KCard(padding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        KText("KeyBox", txt = Txt.Title2)
                        Spacer(Modifier.height(4.dp))
                        KText(
                            "版本 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                            txt = Txt.Footnote2,
                            color = secondaryColor()
                        )
                    }
                    KText("© 2026 ENDDREAM", txt = Txt.Footnote2, color = secondaryColor())
                }
                Spacer(Modifier.height(10.dp))
                KText(
                    "不联网的本地密码管理器，所有数据只在本机保存。\n" +
                            "网络权限仅用于获取一言和图标，与本地密码库无关。",
                    txt = Txt.Body2
                )
                Spacer(Modifier.height(6.dp))
                KText(
                    "密码库由主密码加密，Android 与 Windows 通用。" +
                            "Windows 端暂未进行开发，敬请期待。",
                    txt = Txt.Body2,
                    color = secondaryColor()
                )
            }

            KSection("系统")
            KCard(padding = 0.dp) {
                platform.forEachIndexed { i, lib ->
                    KRow(
                        title = lib.name,
                        subtitle = lib.note,
                        value = lib.version,
                        icon = Icons.Rounded.Build,
                        onClick = { open(lib.url) }
                    )
                    if (i != platform.lastIndex) KDivider(Modifier.padding(start = 56.dp))
                }
            }

            KSection("致谢")
            KCard(padding = 0.dp) {
                libs.forEachIndexed { i, lib ->
                    KRow(
                        title = lib.name,
                        subtitle = "${lib.license} · ${lib.note}",
                        value = lib.version,
                        icon = Icons.Rounded.Code,
                        onClick = { open(lib.url) }
                    )
                    if (i != libs.lastIndex) KDivider(Modifier.padding(start = 56.dp))
                }
            }

        }
    }
}
