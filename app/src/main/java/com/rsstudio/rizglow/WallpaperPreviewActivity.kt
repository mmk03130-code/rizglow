package com.rsstudio.rizglow

import android.app.Activity
import android.app.WallpaperManager
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

/** Full-screen wallpaper preview with "set as wallpaper". */
class WallpaperPreviewActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wallpaper_preview)

        val resId = intent.getIntExtra(EXTRA_RES_ID, 0)
        if (resId == 0) {
            finish()
            return
        }

        findViewById<ImageView>(R.id.preview_image).setImageResource(resId)
        findViewById<TextView>(R.id.btn_close).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btn_set).setOnClickListener {
            try {
                WallpaperManager.getInstance(this).setResource(resId)
                Toast.makeText(this, "Wallpaper set ✨", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this, "Couldn't set wallpaper", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        const val EXTRA_RES_ID = "res_id"
    }
}
