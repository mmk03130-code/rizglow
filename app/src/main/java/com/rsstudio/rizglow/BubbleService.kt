package com.rsstudio.rizglow

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import android.widget.ImageView

/** Floating cat bubble (Themezy-style trigger): drag it anywhere, tap to open
 *  the control center over any app. Needs "Display over other apps". */
class BubbleService : Service() {

    private lateinit var wm: WindowManager
    private var bubble: ImageView? = null
    private var params: WindowManager.LayoutParams? = null

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate() {
        super.onCreate()
        if (!Settings.canDrawOverlays(this)) {
            stopSelf()
            return
        }
        wm = getSystemService(WINDOW_SERVICE) as WindowManager
        val size = dp(60)
        params = WindowManager.LayoutParams(
            size, size,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.END
            x = dp(8)
            y = dp(320)
        }
        val iv = ImageView(this).apply {
            setImageResource(R.drawable.cc_bubble)
            scaleType = ImageView.ScaleType.CENTER_CROP
            // round it
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: android.graphics.Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            clipToOutline = true
            elevation = dp(6).toFloat()
        }
        var downX = 0f; var downY = 0f; var startX = 0; var startY = 0
        var downTime = 0L; var dragging = false
        iv.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = params!!.x; startY = params!!.y
                    downTime = System.currentTimeMillis()
                    dragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (e.rawX - downX).toInt()
                    val dy = (e.rawY - downY).toInt()
                    if (!dragging && (dx * dx + dy * dy) > dp(8) * dp(8)) dragging = true
                    if (dragging) {
                        params!!.x = startX - dx // END gravity: x grows leftward
                        params!!.y = startY + dy
                        wm.updateViewLayout(iv, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!dragging && System.currentTimeMillis() - downTime < 400) {
                        startActivity(
                            Intent(this@BubbleService, ControlCenterActivity::class.java)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                    true
                }
                else -> false
            }
        }
        bubble = iv
        wm.addView(iv, params)
    }

    override fun onDestroy() {
        bubble?.let { try { wm.removeView(it) } catch (e: Exception) {} }
        bubble = null
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        fun isRunning(context: android.content.Context): Boolean {
            val am = context.getSystemService(ACTIVITY_SERVICE) as android.app.ActivityManager
            @Suppress("DEPRECATION")
            return am.getRunningServices(Int.MAX_VALUE)
                .any { it.service.className == BubbleService::class.java.name }
        }
    }
}
