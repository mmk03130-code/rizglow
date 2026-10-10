package com.rsstudio.rizglow

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import kotlin.math.abs

/**
 * Themezy-style trigger: a transparent strip over the status bar watches for a
 * swipe-down gesture and floats the Rizglow control center in. Taps and other
 * gestures pass through untouched, so the status bar keeps working normally.
 */
class ControlCenterAccessService : AccessibilityService() {

    private var strip: FrameLayout? = null
    private var testMode = false
    private lateinit var wm: WindowManager

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addStrip()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onDestroy() {
        strip?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        strip = null
        if (instance === this) instance = null
        super.onDestroy()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (id > 0) resources.getDimensionPixelSize(id) else dp(24)
    }

    private fun addStrip() {
        if (strip != null) return
        // CRITICAL: the system status bar always draws ABOVE accessibility
        // overlays and eats every touch inside its own region. A strip that
        // overlaps the status bar is visible (the bar is translucent) but
        // untouchable — swipes starting on it go straight to the system, which
        // is why the gesture kept opening the native panel instead of ours.
        // So the strip lives fully BELOW the status bar, in app-content
        // territory, where no system window can claim its touches. The gesture
        // is: finger down just under the status bar (top-right), swipe down.
        val h = dp(40)
        // iOS-style corner swipe (like Themezy): 40% width at top-right, so a
        // normal swipe-down elsewhere still opens notifications.
        val w = (resources.displayMetrics.widthPixels * 0.4).toInt()
        val params = WindowManager.LayoutParams(
            w, h,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            y = statusBarHeight() // sit just under the status bar
        }
        val v = FrameLayout(this)
        var downY = 0f
        var downX = 0f
        var tracking = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                // Must claim ACTION_DOWN or the system never delivers MOVE: the
                // swipe would be invisible to us. Note the strip now sits just
                // below the status bar, so while Swipe is ON this corner zone
                // absorbs taps too (swipe pill is opt-in, so this is the
                // user's trade, not a surprise).
                MotionEvent.ACTION_DOWN -> {
                    if (testMode) {
                        android.widget.Toast.makeText(this@ControlCenterAccessService,
                            "Strip got your tap!", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    // Unarmed: stay completely out of the way (native UX untouched).
                    if (!isSwipeOpen(this@ControlCenterAccessService)) {
                        tracking = false
                        false
                    } else {
                        downY = e.rawY
                        downX = e.rawX
                        tracking = true
                        true
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (tracking && isSwipeOpen(this) && !ControlCenterGate.panelOpen) {
                        val dy = e.rawY - downY
                        val dx = e.rawX - downX
                        if (dy > dp(20) && dy > 2 * abs(dx)) {
                            tracking = false
                            android.widget.Toast.makeText(this@ControlCenterAccessService,
                                "Swipe detected — opening panel…",
                                android.widget.Toast.LENGTH_SHORT).show()
                            openPanel()
                        }
                    }
                    true
                }
                else -> true
            }
        }
        strip = v
        try {
            wm.addView(v, params)
        } catch (e: Exception) {
            strip = null
        }
    }

    /** Test hook: red strip + tap feedback, so touch delivery is observable. */
    private fun flashStrip(): Boolean {
        val v = strip ?: return false
        testMode = true
        v.setBackgroundColor(0x66FF0000)
        v.postDelayed({
            testMode = false
            v.setBackgroundColor(0x00000000)
        }, 5000)
        return true
    }

    private fun openPanel() {
        try {
            startActivity(
                Intent(this, ControlCenterActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (e: Exception) {
            android.widget.Toast.makeText(this,
                "Panel launch blocked: ${e.javaClass.simpleName}",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    companion object {
        private const val PREFS = "rizglow_cc"
        private const val KEY_SWIPE = "swipe_open"
        private var instance: ControlCenterAccessService? = null

        fun isSwipeOpen(ctx: Context): Boolean =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_SWIPE, false)

        fun setSwipeOpen(ctx: Context, v: Boolean) {
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_SWIPE, v).apply()
        }

        fun openNotifications(): Boolean =
            instance?.performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS) == true

        /** True when the system has actually bound+connected the service. */
        fun isServiceConnected(): Boolean = instance != null

        fun isStripAttached(): Boolean = instance?.strip != null

        fun flashStrip(): Boolean = instance?.flashStrip() == true

        fun isEnabled(ctx: Context): Boolean {
            val expected = ComponentName(ctx, ControlCenterAccessService::class.java).flattenToString()
            val enabled =
                Settings.Secure.getString(ctx.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                    ?: ""
            return enabled.split(':').any { it == expected }
        }
    }
}

/** Shared flag so the swipe strip stands down while the panel is open. */
object ControlCenterGate {
    var panelOpen = false
}
