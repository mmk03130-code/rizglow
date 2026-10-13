package com.rsworks.rizglow

import android.app.Activity
import android.app.AlertDialog
import android.app.WallpaperManager
import android.os.Build
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.rsworks.rizglow.ads.UnlockManager
import kotlin.math.abs

/** Full-screen wallpaper preview: swipe through the bundle's wallpapers,
 *  heart favorites, and apply to Home / Lock / Both. */
class WallpaperPreviewActivity : Activity() {

    private var ids: IntArray = intArrayOf()
    private var index: Int = 0
    private var lockedBundleId: String? = null
    private lateinit var image: ImageView
    private lateinit var counter: TextView
    private lateinit var heart: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_preview)

        ids = intent.getIntArrayExtra(EXTRA_RES_IDS) ?: intArrayOf()
        val single = intent.getIntExtra(EXTRA_RES_ID, 0)
        if (ids.isEmpty() && single != 0) ids = intArrayOf(single)
        if (ids.isEmpty()) {
            finish()
            return
        }
        index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(ids.indices)
        lockedBundleId = intent.getStringExtra(EXTRA_LOCKED_BUNDLE)

        image = findViewById(R.id.preview_image)
        counter = findViewById(R.id.preview_counter)
        heart = findViewById(R.id.btn_fav)

        refresh()

        // Swipe left/right to browse.
        val detector = GestureDetector(this,
            object : GestureDetector.SimpleOnGestureListener() {
                override fun onFling(
                    e1: MotionEvent?, e2: MotionEvent,
                    velocityX: Float, velocityY: Float
                ): Boolean {
                    if (e1 == null) return false
                    val dx = e2.x - e1.x
                    if (abs(dx) > abs(e2.y - e1.y) && abs(dx) > 120 && abs(velocityX) > 400) {
                        if (dx < 0) index = (index + 1).coerceAtMost(ids.lastIndex)
                        else index = (index - 1).coerceAtLeast(0)
                        refresh()
                        return true
                    }
                    return false
                }
            })
        image.setOnTouchListener { _, e -> detector.onTouchEvent(e) }

        findViewById<TextView>(R.id.btn_close).setOnClickListener { finish() }
        heart.setOnClickListener {
            val nowFav = FavoritesManager.toggle(this, ids[index])
            heart.text = if (nowFav) "❤" else "🤍"
        }
        val setBtn = findViewById<TextView>(R.id.btn_set)
        if (lockedBundleId != null) {
            setBtn.text = "🔒 Unlock to set"
        }
        setBtn.setOnClickListener {
            val locked = lockedBundleId
            if (locked != null) {
                // Desire before paywall: they saw it, now unlock to use it.
                UnlockManager.promptUnlock(this, locked)
            } else {
                showApplyDialog()
            }
        }
    }

    private fun refresh() {
        image.setImageResource(ids[index])
        counter.text = "${index + 1} / ${ids.size}"
        heart.text = if (FavoritesManager.isFavorite(this, ids[index])) "❤" else "🤍"
    }

    private fun showApplyDialog() {
        val resId = ids[index]
        val wm = WallpaperManager.getInstance(this)
        val apply = { which: Int ->
            try {
                if (Build.VERSION.SDK_INT >= 24) {
                    val bitmap = android.graphics.BitmapFactory.decodeResource(resources, resId)
                    when (which) {
                        0 -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_SYSTEM)
                        1 -> wm.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK)
                        else -> wm.setBitmap(bitmap, null, true,
                            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK)
                    }
                } else {
                    wm.setResource(resId)
                }
                Toast.makeText(this, "Wallpaper set ✨", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't set wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
        AlertDialog.Builder(this)
            .setTitle("Set wallpaper")
            .setItems(arrayOf("Home screen", "Lock screen", "Both")) { _, which -> apply(which) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    companion object {
        const val EXTRA_RES_ID = "res_id"
        const val EXTRA_RES_IDS = "res_ids"
        const val EXTRA_INDEX = "index"
        const val EXTRA_LOCKED_BUNDLE = "locked_bundle"
    }
}
