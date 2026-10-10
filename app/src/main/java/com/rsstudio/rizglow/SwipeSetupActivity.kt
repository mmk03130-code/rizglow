package com.rsstudio.rizglow

import android.app.Activity
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/** Guided "How to enable" flow for the swipe-to-open trigger (mirrors the
 *  pattern top control-center apps use): restricted-settings step, the
 *  accessibility toggle, then verification. */
class SwipeSetupActivity : Activity() {

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(getColor(R.color.bg))
            setPadding(dp(22), dp(28), dp(22), dp(22))
        }
        root.addView(TextView(this).apply {
            text = "Swipe to open"
            textSize = 22f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(getColor(R.color.text))
        })
        root.addView(TextView(this).apply {
            text = "Open the fuzzy panel with a swipe down from the top-right corner, just like the big theme apps."
            textSize = 14f
            setTextColor(getColor(R.color.muted))
            setPadding(0, dp(6), 0, dp(14))
        })
        statusView = TextView(this).apply {
            textSize = 15f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(dp(14), dp(10), dp(14), dp(10))
        }
        root.addView(statusView)

        val steps = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            ).apply { topMargin = dp(10) }
        }
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val step = { n: String, title: String, body: String ->
            list.addView(TextView(this).apply {
                text = n
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(0xFFB388FF.toInt())
                setPadding(0, dp(12), 0, dp(2))
            })
            list.addView(TextView(this).apply {
                text = title
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(getColor(R.color.text))
            })
            list.addView(TextView(this).apply {
                text = body
                textSize = 14f
                setTextColor(getColor(R.color.muted))
                setPadding(0, dp(2), 0, 0)
            })
        }
        step("STEP 1", "Allow restricted settings",
            "Android blocks sideloaded apps here. Go to Settings → Apps → Rizglow → ⋮ (top-right) → \"Allow restricted settings\" → Allow.")
        step("STEP 2", "Turn on the service",
            "In Accessibility → Downloaded apps, open \"Rizglow Panel\" and turn it ON. Accept the system prompt.")
        step("STEP 3", "Test the swipe",
            "Come back and swipe down from the top-right corner of your screen. The fuzzy panel should float in.")
        steps.addView(list)
        root.addView(steps)

        root.addView(TextView(this).apply {
            text = "Privacy: Rizglow only watches for that swipe gesture. It does not read your screen or collect any data."
            textSize = 12f
            setTextColor(getColor(R.color.muted))
            setPadding(dp(4), dp(12), dp(4), dp(12))
            gravity = Gravity.CENTER
        })

        val goBtn = TextView(this).apply {
            text = "Go to Settings →"
            textSize = 16f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(14), dp(16), dp(14))
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(24).toFloat()
                setColor(0xFF9B6BD3.toInt())
            }
            isClickable = true
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(goBtn)
        val doneBtn = TextView(this).apply {
            text = "Done"
            textSize = 15f
            setTextColor(getColor(R.color.muted))
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(12), dp(16), dp(4))
            isClickable = true
            setOnClickListener { finish() }
        }
        root.addView(doneBtn)
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        val on = ControlCenterAccessService.isEnabled(this)
        if (on) {
            ControlCenterAccessService.setSwipeOpen(this, true)
            statusView.text = "● Service is ON — swipe from the top-right corner!"
            statusView.setTextColor(0xFF7BC98A.toInt())
            statusView.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(0x227BC98A.toInt())
            }
        } else {
            statusView.text = "○ Service is OFF — follow the steps below"
            statusView.setTextColor(0xFFE0A05A.toInt())
            statusView.background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = dp(12).toFloat()
                setColor(0x22E0A05A.toInt())
            }
            Toast.makeText(this, "Service still off — complete steps 1–2", Toast.LENGTH_SHORT).show()
        }
    }
}
