package com.enddream.keybox

import android.app.Activity
import android.os.Bundle
import android.view.autofill.AutofillId
import android.view.autofill.AutofillManager
import android.view.autofill.AutofillValue
import android.widget.RemoteViews
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FillAuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val mode = intent.getStringExtra(Autofill.EXTRA_MODE) ?: Autofill.MODE_AUTOFILL
        val entryId = intent.getStringExtra(Autofill.EXTRA_ENTRY) ?: ""
        setContent {
            KeyBoxTheme(UiStyle.Material, Prefs.seed(this), false) {
                MaterialTheme {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        FillAuth(entryId, mode) { finish() }
                    }
                }
            }
        }
    }

    fun complete(mode: String) {
        if (mode == Autofill.MODE_QUICK) {
            setResult(Activity.RESULT_OK)
            finish()
            return
        }
        val ids = intent.getParcelableArrayListExtra<AutofillId>("ids") ?: ArrayList()
        val uid = intent.getParcelableExtra<AutofillId>("uid")
        val pid = intent.getParcelableExtra<AutofillId>("pid")
        val pkg = intent.getStringExtra("pkg") ?: ""
        val host = intent.getStringExtra("host") ?: ""
        val typed = intent.getStringExtra("typed") ?: ""
        val resp = android.service.autofill.FillResponse.Builder()
        var any = false
        Autofill.candidates(this, host, pkg)
            .filter { Autofill.startsWithQuery(it, typed) }
            .take(6)
            .forEach { e ->
                val title = RemoteViews(packageName, R.layout.autofill_item)
                title.setTextViewText(
                    R.id.autofill_text,
                    e.displayTitle() + if (e.user.isNullOrEmpty()) "" else "  ·  " + e.user
                )
                val ds = android.service.autofill.Dataset.Builder(title)
                if (uid != null) ds.setValue(uid, AutofillValue.forText(e.user ?: ""), title)
                if (pid != null) ds.setValue(pid, AutofillValue.forText(e.pass ?: ""), title)
                resp.addDataset(ds.build())
                any = true
            }
        if (!any) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        val result = android.content.Intent()
        result.putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, resp.build())
        setResult(Activity.RESULT_OK, result)
        finish()
    }
}

@Composable
private fun FillAuth(entryId: String, mode: String, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val act = ctx as? FillAuthActivity
    var pw by remember { mutableStateOf("") }
    var err by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val bioOn = remember { Bio.isEnabled(ctx) }

    fun deliver(master: String) {
        if (busy) return
        busy = true
        err = null
        scope.launch {
            try {
                if (!Store.open) {
                    withContext(Dispatchers.IO) { Store.unlock(ctx, master) }
                } else if (Store.master != null && Store.master != master) {
                    throw IllegalStateException("主密码不正确")
                }
                if (entryId.isNotEmpty()) {
                    val e = Store.find(entryId) ?: throw IllegalStateException("条目不存在")
                    FillBridge.onAuth?.invoke(e.user ?: "", e.pass ?: "")
                    FillBridge.onAuth = null
                }
                act?.complete(mode)
                onDone()
            } catch (t: Throwable) {
                err = t.message ?: "验证失败"
                pw = ""
            }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        val a = act ?: return@LaunchedEffect
        if (bioOn && !Store.open) {
            Bio.authenticate(a, { m -> deliver(m) }, { msg ->
                if (msg != "已取消") err = msg
            })
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        KText("验证身份后填充", txt = Txt.Title3)
        Spacer(Modifier.height(6.dp))
        KText("确认是你本人在使用这条密码", txt = Txt.Footnote2, color = secondaryColor())
        Spacer(Modifier.height(16.dp))
        KField(pw, { pw = it }, "主密码", Modifier.fillMaxWidth(), password = true, enabled = !busy)
        if (err != null) {
            Spacer(Modifier.height(8.dp))
            KText(err!!, txt = Txt.Footnote2, color = errorColor())
        }
        Spacer(Modifier.height(14.dp))
        KButton(
            "确认填充",
            { deliver(pw) },
            Modifier.fillMaxWidth(),
            enabled = !busy && (pw.isNotEmpty() || Store.master == null)
        )
        if (bioOn) {
            Spacer(Modifier.height(6.dp))
            KTextButton("使用生物识别", {
                val a = act
                if (a != null) Bio.authenticate(a, { m -> deliver(m) }, { msg ->
                    if (msg != "已取消") err = msg
                })
            }, Modifier.fillMaxWidth())
        }
        Spacer(Modifier.height(4.dp))
        KTextButton("取消", { onDone() }, Modifier.fillMaxWidth())
    }
}
