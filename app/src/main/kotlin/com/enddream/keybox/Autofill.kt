package com.enddream.keybox

import android.app.PendingIntent
import android.app.assist.AssistStructure
import android.content.Context
import android.content.Intent
import android.os.CancellationSignal
import android.provider.Settings
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveInfo
import android.service.autofill.SaveRequest
import android.text.InputType
import android.view.autofill.AutofillId
import android.widget.RemoteViews
import com.enddream.keybox.core.Entry
import java.net.URI

object Autofill {
    const val EXTRA_ENTRY = "entry_id"
    const val EXTRA_MODE = "mode"
    const val MODE_AUTOFILL = "autofill"
    const val MODE_QUICK = "quick"

    private const val PREFS = "keybox_autofill"

    fun serviceEnabled(c: Context): Boolean = try {
        val am = c.getSystemService(android.view.autofill.AutofillManager::class.java)
        am != null && am.hasEnabledAutofillServices()
    } catch (t: Throwable) {
        false
    }

    fun quickFillEnabled(c: Context): Boolean {
        if (!c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("quick", false)) return false
        val enabled = Settings.Secure.getString(
            c.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabled.contains(c.packageName + "/" + QuickFillService::class.java.name) ||
                enabled.contains(c.packageName + "/.QuickFillService")
    }

    fun setQuickWanted(c: Context, on: Boolean) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("quick", on).apply()
    }

    fun openAutofillSettings(c: Context) {
        val i = Intent(Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE)
        i.data = android.net.Uri.parse("package:" + c.packageName)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            c.startActivity(i)
        } catch (t: Throwable) {
            openAppSettings(c)
        }
    }

    fun openAccessibilitySettings(c: Context) {
        val i = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            c.startActivity(i)
        } catch (t: Throwable) {
            openAppSettings(c)
        }
    }

    fun matches(e: Entry, host: String, pkg: String): Boolean {
        if (pkg.isNotEmpty() && (e.pkg ?: "").equals(pkg, true)) return true
        if (host.isEmpty()) return false
        val h = host.lowercase().removePrefix("www.")
        return e.urlList().any { u ->
            val hu = hostOf(u)
            hu.isNotEmpty() && (hu == h || hu.endsWith("." + h) || h.endsWith("." + hu))
        }
    }

    fun candidates(c: Context, host: String, pkg: String): List<Entry> =
        Store.vault.entries.filter { it.del == 0 && matches(it, host, pkg) }

    fun startsWithQuery(e: Entry, typed: String): Boolean {
        val q = typed.trim()
        if (q.isEmpty()) return true
        val user = e.user ?: ""
        val title = e.title ?: ""
        return user.contains(q, true) || title.contains(q, true)
    }

    fun hostOf(uri: String): String {
        if (uri.isEmpty()) return ""
        return try {
            val u = URI(if (uri.contains("://")) uri else "https://$uri")
            (u.host ?: "").lowercase().removePrefix("www.")
        } catch (t: Throwable) {
            uri.lowercase().removePrefix("www.").substringBefore('/')
        }
    }

    fun pendingIntent(
        c: Context,
        ids: List<AutofillId>,
        user: AutofillId?,
        pass: AutofillId?,
        pkg: String,
        host: String,
        typed: String
    ): PendingIntent {
        val i = Intent(c, FillAuthActivity::class.java)
        i.putExtra(EXTRA_MODE, MODE_AUTOFILL)
        i.putExtra("pkg", pkg)
        i.putExtra("host", host)
        i.putExtra("typed", typed)
        i.putParcelableArrayListExtra("ids", ArrayList(ids))
        if (user != null) i.putExtra("uid", user)
        if (pass != null) i.putExtra("pid", pass)
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        return PendingIntent.getActivity(c, (pkg + host).hashCode(), i, flags)
    }

    fun buildResponse(
        c: Context,
        request: FillRequest,
        ids: List<AutofillId>,
        user: AutofillId?,
        pass: AutofillId?,
        typed: String
    ): FillResponse? {
        if (ids.isEmpty()) return null
        val contexts = request.fillContexts
        val structure = contexts.lastOrNull()?.structure ?: return null
        val pkgName = structure.activityComponent?.packageName ?: ""
        var webDomain = ""
        try {
            if (structure.windowNodeCount > 0) {
                webDomain = structure.getWindowNodeAt(0).rootViewNode.webDomain ?: ""
            }
        } catch (t: Throwable) {
        }
        val host = webDomain
        val pkg = if (webDomain.isEmpty()) pkgName else ""
        if (candidates(c, host, pkg).none { startsWithQuery(it, typed) }) return null

        val builder = FillResponse.Builder()
        val rv = RemoteViews(c.packageName, R.layout.autofill_item)
        rv.setTextViewText(R.id.autofill_text, "验证后填充 KeyBox 条目")
        val pi = pendingIntent(c, ids, user, pass, pkg, host, typed)
        builder.setAuthentication(ids.toTypedArray(), pi.intentSender, rv)
        if (user != null && pass != null) {
            builder.setSaveInfo(
                SaveInfo.Builder(SaveInfo.SAVE_DATA_TYPE_PASSWORD, arrayOf(user, pass)).build()
            )
        }
        return builder.build()
    }
}

class KeyBoxAutofillService : AutofillService() {
    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        try {
            val contexts = request.fillContexts
            if (contexts.isEmpty()) {
                callback.onSuccess(null)
                return
            }
            val view = collect(contexts.last().structure) ?: run {
                callback.onSuccess(null)
                return
            }
            val user = view.user
            val pass = view.pass
            if (user == null || pass == null) {
                callback.onSuccess(null)
                return
            }
            callback.onSuccess(
                Autofill.buildResponse(this, request, view.ids, user, pass, view.userText)
            )
        } catch (t: Throwable) {
            callback.onSuccess(null)
        }
    }

    private class Found {
        val ids = mutableListOf<AutofillId>()
        var user: AutofillId? = null
        var pass: AutofillId? = null
        var userText: String = ""
    }

    private fun collect(structure: AssistStructure): Found {
        val f = Found()
        for (i in 0 until structure.windowNodeCount) {
            walk(structure.getWindowNodeAt(i).rootViewNode, f)
        }
        return f
    }

    private fun walk(node: AssistStructure.ViewNode, f: Found) {
        val inputType = node.inputType
        val klass = node.className ?: ""
        val isPassword = (inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (inputType and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                (inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0 ||
                (inputType and InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) != 0
        val id = (node.idEntry ?: "").lowercase()
        val hint = (node.hint ?: "").lowercase()
        val isUser = (inputType and InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS) != 0 ||
                (inputType and InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS) != 0 ||
                hint.contains("user") || hint.contains("account") ||
                hint.contains("邮箱") || hint.contains("账号") || hint.contains("手机") ||
                id.contains("user") || id.contains("email") || id.contains("account") ||
                id.contains("login") || id.contains("phone")
        val usable = isPassword || isUser ||
                klass.contains("EditText") || klass.contains("TextInputEditText")
        val aid = node.autofillId
        if (usable && isUser && node.text != null) f.userText = node.text.toString()
        if (usable && aid != null) {
            f.ids.add(aid)
            if (isPassword && f.pass == null) f.pass = aid
            if (isUser && f.user == null) f.user = aid
        }
        for (i in 0 until node.childCount) {
            walk(node.getChildAt(i), f)
        }
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        callback.onSuccess()
    }
}
