package com.rsstudio.rizglow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView

/** 3-page intro: Wallpapers → Icons → Widgets. */
class OnboardingActivity : Activity() {

    private var page = 0
    private lateinit var pages: List<LinearLayout>
    private lateinit var dots: List<View>
    private lateinit var btnNext: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pages = listOf(
            findViewById(R.id.page1),
            findViewById(R.id.page2),
            findViewById(R.id.page3)
        )
        dots = listOf(
            findViewById(R.id.dot1),
            findViewById(R.id.dot2),
            findViewById(R.id.dot3)
        )
        btnNext = findViewById(R.id.btn_next)

        findViewById<TextView>(R.id.btn_skip).setOnClickListener { finishOnboarding() }
        btnNext.setOnClickListener {
            if (page < 2) showPage(page + 1) else finishOnboarding()
        }
        showPage(0)
    }

    private fun showPage(i: Int) {
        page = i
        pages.forEachIndexed { idx, v -> v.visibility = if (idx == i) View.VISIBLE else View.GONE }
        dots.forEachIndexed { idx, d ->
            d.setBackgroundResource(
                if (idx == i) R.drawable.dot_selected else R.drawable.dot_unselected
            )
        }
        btnNext.text = if (i == 2) "Get Started" else "Next"
    }

    private fun finishOnboarding() {
        getSharedPreferences(SplashActivity.PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(SplashActivity.KEY_ONBOARDED, true).apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
