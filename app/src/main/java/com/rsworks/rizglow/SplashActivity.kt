package com.rsworks.rizglow

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView

/** Entry screen: Rizglow™ logo fade-in + "Made by RS Works" credit. */
class SplashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        findViewById<ImageView>(R.id.splash_logo)
            .animate().alpha(1f).setDuration(900).start()

        Handler(Looper.getMainLooper()).postDelayed({
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val next = if (prefs.getBoolean(KEY_ONBOARDED, false)) {
                Intent(this, MainActivity::class.java)
            } else {
                Intent(this, OnboardingActivity::class.java)
            }
            startActivity(next)
            finish()
        }, 1500)
    }

    companion object {
        const val PREFS = "rizglow_prefs"
        const val KEY_ONBOARDED = "onboarded"
    }
}
