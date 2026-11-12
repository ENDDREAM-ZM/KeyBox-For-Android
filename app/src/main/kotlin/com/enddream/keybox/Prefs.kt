package com.enddream.keybox

import android.content.Context

object Prefs {
    private const val NAME = "keybox"

    fun get(c: Context) = c.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun lockMinutes(c: Context): Int {
        val v = get(c).getInt("lock_min", 5)
        return if (v == 15 || v == 30) v else 5
    }

    fun setLockMinutes(c: Context, v: Int) = get(c).edit().putInt("lock_min", v).apply()

    fun retentionDays(c: Context): Int {
        val v = get(c).getInt("retention_days", 7)
        return if (v == 15 || v == 30) v else 7
    }

    fun setRetentionDays(c: Context, v: Int) = get(c).edit().putInt("retention_days", v).apply()

    fun biometric(c: Context) = get(c).getBoolean("biometric", false)
    fun setBiometric(c: Context, v: Boolean) = get(c).edit().putBoolean("biometric", v).apply()

    fun seed(c: Context) = get(c).getInt("seed", 0xFF006A60.toInt())
    fun setSeed(c: Context, v: Int) = get(c).edit().putInt("seed", v).apply()

    fun dynamic(c: Context) = get(c).getBoolean("dynamic", false)
    fun setDynamic(c: Context, v: Boolean) = get(c).edit().putBoolean("dynamic", v).apply()

    fun style(c: Context) = get(c).getString("style", "material") ?: "material"
    fun setStyle(c: Context, v: String) = get(c).edit().putString("style", v).apply()

    fun themeMode(c: Context) = get(c).getString("theme_mode", "system") ?: "system"
    fun setThemeMode(c: Context, v: String) = get(c).edit().putString("theme_mode", v).apply()

    fun screenshot(c: Context) = get(c).getBoolean("screenshot", false)
    fun setScreenshot(c: Context, v: Boolean) = get(c).edit().putBoolean("screenshot", v).apply()

    fun clipboardSeconds(c: Context): Int {
        val v = get(c).getInt("clip_sec", 30)
        return if (v == 0 || v == 60 || v == 300) v else 30
    }

    fun setClipboardSeconds(c: Context, v: Int) = get(c).edit().putInt("clip_sec", v).apply()

    fun noMasterRemind(c: Context) = get(c).getBoolean("no_master_remind", false)
    fun setNoMasterRemind(c: Context, v: Boolean) = get(c).edit().putBoolean("no_master_remind", v).apply()

    fun showSystemApps(c: Context) = get(c).getBoolean("show_sys_apps", false)
    fun setShowSystemApps(c: Context, v: Boolean) =
        get(c).edit().putBoolean("show_sys_apps", v).apply()

    fun bannerOff(c: Context) = get(c).getBoolean("banner_off", false)
    fun setBannerOff(c: Context, v: Boolean) = get(c).edit().putBoolean("banner_off", v).apply()
}
