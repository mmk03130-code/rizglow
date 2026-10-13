package com.rsworks.rizglow

import android.accessibilityservice.AccessibilityService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.os.Build
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
    private var panelView: ControlCenterPanelView? = null
    private var testMode = false
    private lateinit var wm: WindowManager

    override fun onServiceConnected() {
        instance = this
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        addStrip()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Foldable unfolded, rotated, or resized: rebuild the panel at the
        // new size and re-anchor the strip (its 40% was for the old width).
        if (panelView != null) {
            hidePanel()
            showPanel()
        }
        strip?.let {
            try { wm.removeView(it) } catch (e: Exception) {}
            strip = null
            addStrip()
        }
    }

    override fun onDestroy() {
        hidePanel()
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
        // Float the panel in as an instant system overlay (like the big theme
        // apps) — no activity launch, no transition, no flakiness.
        showPanel()
    }

    /** Show the panel as a system overlay. The service knows exactly when it's
     *  up, so the strip gate can't get stuck like the old activity flag did. */
    fun showPanel() {
        if (panelView != null) return
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )
        val v = ControlCenterPanelView(this, { hidePanel() }, compact = true)
        v.refresh()
        panelView = v
        ControlCenterGate.panelOpen = true
        try {
            wm.addView(v, params)
            v.post { v.playEntrance() }
        } catch (e: Exception) {
            panelView = null
            ControlCenterGate.panelOpen = false
            android.widget.Toast.makeText(this,
                "Panel overlay blocked: ${e.javaClass.simpleName}",
                android.widget.Toast.LENGTH_LONG).show()
        }
    }

    fun hidePanel() {
        val v = panelView ?: return
        panelView = null
        ControlCenterGate.panelOpen = false
        try { wm.removeView(v) } catch (e: Exception) {}
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

/** Screen width in dp, safe from a Service on any API level. Uses *current*
 *  metrics (not maximum) so closed foldables and split-screen report right. */
fun screenWidthDp(context: Context): Int {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val density = context.resources.displayMetrics.density
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        (wm.currentWindowMetrics.bounds.width() / density).toInt()
    } else {
        val dm = android.util.DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(dm)
        (dm.widthPixels / dm.density).toInt()
    }
}

/** Panel width: 12dp margins, capped at 400dp so it fills the screen
 *  edge-to-edge like the top theme apps. */
fun panelWidthDp(context: Context): Int =
    minOf(400, screenWidthDp(context) - 24).coerceAtLeast(200)
