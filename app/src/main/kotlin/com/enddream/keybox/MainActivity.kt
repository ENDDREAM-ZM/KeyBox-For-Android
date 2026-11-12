package com.enddream.keybox

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

class MainActivity : ComponentActivity() {
    private var bgAt = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        applySecure()
        setContent { KeyBoxRoot() }
    }

    override fun onStart() {
        super.onStart()
        if (bgAt > 0L && Store.open && Store.master != null) {
            val limit = Prefs.lockMinutes(this) * 60_000L
            if (System.currentTimeMillis() - bgAt >= limit) {
                Store.lock()
                AppState.locked = true
            }
        }
        bgAt = 0L
    }

    override fun onStop() {
        super.onStop()
        bgAt = System.currentTimeMillis()
    }

    override fun onResume() {
        super.onResume()
        applySecure()
    }

    private fun applySecure() {
        if (Prefs.screenshot(this)) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        } else {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
