package com.enddream.keybox

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.enddream.keybox.core.Entry

object FillBridge {
    var onAuth: ((String, String) -> Unit)? = null
}

class QuickFillService : AccessibilityService() {
    private val handler = Handler(Looper.getMainLooper())
    private var chip: View? = null
    private var panel: View? = null
    private var wm: WindowManager? = null
    private var lastPkg = ""
    private var entries: List<Entry> = emptyList()
    private var anchorRect: Rect? = null

    private val scan = Runnable { doScan() }

    override fun onServiceConnected() {
        super.onServiceConnected()
        wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (!Autofill.quickFillEnabled(this)) {
            hideAll()
            return
        }
        val t = event.eventType
        if (t != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            t != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            t != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            t != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
        ) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") {
            hideAll()
            return
        }
        lastPkg = pkg
        handler.removeCallbacks(scan)
        handler.postDelayed(scan, 220)
    }

    private fun editableOf(n: AccessibilityNodeInfo?): Boolean =
        n != null && (n.isEditable || (n.className?.contains("EditText") == true))

    private fun isPassword(n: AccessibilityNodeInfo): Boolean {
        val t = n.inputType
        return (t and InputType.TYPE_TEXT_VARIATION_PASSWORD) != 0 ||
                (t and InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD) != 0 ||
                (t and InputType.TYPE_NUMBER_VARIATION_PASSWORD) != 0
    }

    private fun collectEditable(root: AccessibilityNodeInfo?): List<AccessibilityNodeInfo> {
        val out = mutableListOf<AccessibilityNodeInfo>()
        fun walk(n: AccessibilityNodeInfo?) {
            if (n == null) return
            if (editableOf(n)) out.add(n)
            for (i in 0 until n.childCount) {
                try {
                    walk(n.getChild(i))
                } catch (t: Throwable) {
                }
            }
        }
        walk(root)
        return out
    }

    private fun doScan() {
        hideAll()
        val root = try {
            rootInActiveWindow
        } catch (t: Throwable) {
            null
        } ?: return
        val fields = collectEditable(root)
        if (fields.isEmpty()) return
        if (fields.none { isPassword(it) }) return
        val focused = try {
            root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        } catch (t: Throwable) {
            null
        }
        if (focused == null || !editableOf(focused)) return
        val pkg = root.packageName?.toString() ?: lastPkg
        val userField = fields.firstOrNull { !isPassword(it) }
        if (userField == null || focused != userField) return
        val typed = focused.text?.toString()?.trim() ?: ""
        if (typed.isEmpty()) return
        val list = Autofill.candidates(this, "", pkg).filter { Autofill.startsWithQuery(it, typed) }
        if (list.isEmpty()) return
        entries = list
        showChip(focused)
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    private fun iconBitmap(sizeDp: Int): Bitmap? {
        return try {
            val d: Drawable = packageManager.getApplicationIcon(packageName)
            val px = dp(sizeDp)
            val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            d.setBounds(0, 0, px, px)
            d.draw(c)
            b
        } catch (t: Throwable) {
            null
        }
    }

    private fun boundsOf(n: AccessibilityNodeInfo): Rect? {
        val r = Rect()
        try {
            n.getBoundsInScreen(r)
        } catch (t: Throwable) {
            return null
        }
        if (r.width() <= 0 || r.height() <= 0) return null
        return r
    }

    private fun belowOrAbove(rect: Rect, height: Int): Int {
        val screenH = resources.displayMetrics.heightPixels
        val below = rect.bottom + dp(6)
        return if (below + height < screenH - dp(24)) below
        else (rect.top - height - dp(6)).coerceAtLeast(dp(24))
    }

    private fun showChip(anchor: AccessibilityNodeInfo) {
        val w = wm ?: return
        val ctx = this
        val rect = boundsOf(anchor) ?: return
        anchorRect = rect
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(10), dp(6), dp(14), dp(6))
            background = GradientDrawable().apply {
                cornerRadius = dp(10).toFloat()
                setColor(0xFF1A56C4.toInt())
            }
            setOnClickListener {
                hideChip()
                showPanel()
            }
        }
        iconBitmap(20)?.let {
            box.addView(ImageView(ctx).apply {
                setImageBitmap(it)
                scaleType = ImageView.ScaleType.FIT_CENTER
                layoutParams = LinearLayout.LayoutParams(dp(20), dp(20)).apply {
                    marginEnd = dp(8)
                }
            })
        }
        box.addView(TextView(ctx).apply {
            text = "KeyBox"
            setTextColor(Color.WHITE)
            textSize = 13f
        })
        val lp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        lp.x = rect.left.coerceAtLeast(dp(4))
        lp.y = belowOrAbove(rect, dp(40))
        try {
            w.addView(box, lp)
            chip = box
        } catch (t: Throwable) {
        }
    }

    private fun showPanel() {
        val w = wm ?: return
        val ctx = this
        val rect = anchorRect
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(4), dp(4), dp(4), dp(4))
            background = GradientDrawable().apply {
                cornerRadius = dp(14).toFloat()
                setColor(Color.WHITE)
                setStroke(dp(1), 0xFFE2E2E2.toInt())
            }
        }
        entries.take(8).forEach { e ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(10), dp(9), dp(10), dp(9))
                setOnClickListener {
                    hideAll()
                    pick(e)
                }
            }
            IconLoader.cached(e)?.let {
                row.addView(ImageView(ctx).apply {
                    setImageBitmap(it)
                    layoutParams = LinearLayout.LayoutParams(dp(26), dp(26)).apply {
                        marginEnd = dp(10)
                    }
                })
            }
            row.addView(TextView(ctx).apply {
                text = e.displayTitle() + if (e.user.isNullOrEmpty()) "" else "  ·  " + e.user
                setTextColor(0xFF1A1A1A.toInt())
                textSize = 15f
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            })
            box.addView(row)
        }
        val lp = WindowManager.LayoutParams(
            dp(232),
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            android.graphics.PixelFormat.TRANSLUCENT
        )
        lp.gravity = Gravity.TOP or Gravity.START
        if (rect != null) {
            lp.x = rect.left.coerceAtLeast(dp(4))
            lp.x = lp.x.coerceAtMost(resources.displayMetrics.widthPixels - dp(236))
            lp.y = belowOrAbove(rect, dp(232))
        } else {
            lp.x = dp(12)
            lp.y = dp(160)
        }
        try {
            w.addView(box, lp)
            panel = box
        } catch (t: Throwable) {
        }
    }

    private fun pick(e: Entry) {
        if (Store.open) {
            fill(e.user ?: "", e.pass ?: "")
            return
        }
        FillBridge.onAuth = { u, p -> fill(u, p) }
        val i = Intent(this, FillAuthActivity::class.java)
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        i.putExtra(Autofill.EXTRA_ENTRY, e.id)
        i.putExtra(Autofill.EXTRA_MODE, Autofill.MODE_QUICK)
        try {
            startActivity(i)
        } catch (t: Throwable) {
        }
    }

    private fun hideChip() {
        chip?.let {
            try {
                wm?.removeView(it)
            } catch (t: Throwable) {
            }
        }
        chip = null
    }

    private fun hideAll() {
        hideChip()
        panel?.let {
            try {
                wm?.removeView(it)
            } catch (t: Throwable) {
            }
        }
        panel = null
    }

    private fun fill(user: String, pass: String) {
        val root = try {
            rootInActiveWindow
        } catch (t: Throwable) {
            null
        }
        val fields = collectEditable(root)
        if (fields.isEmpty()) {
            Toast.makeText(this, "没找到输入框", Toast.LENGTH_SHORT).show()
            return
        }
        val passField = fields.firstOrNull { isPassword(it) }
        val userField = fields.firstOrNull { !isPassword(it) }
        if (user.isNotEmpty()) setText(userField, user)
        if (pass.isNotEmpty()) setText(passField ?: fields.lastOrNull(), pass)
    }

    private fun setText(n: AccessibilityNodeInfo?, text: String) {
        if (n == null || text.isEmpty()) return
        try {
            n.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val b = Bundle()
            b.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            n.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, b)
        } catch (t: Throwable) {
        }
    }

    override fun onInterrupt() {
        hideAll()
    }

    override fun onDestroy() {
        hideAll()
        super.onDestroy()
    }
}
