package com.rsstudio.rizglow

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.rsstudio.rizglow.ads.UnlockManager
import com.rsstudio.rizglow.model.BundleRepository
import com.rsstudio.rizglow.model.RizBundle

/** Main app: 4 tabs (Discover / Wallpapers / Icons / Widgets) + bundle cards. */
class MainActivity : Activity() {

    private lateinit var pages: List<View>
    private lateinit var dots: List<View>
    private lateinit var labels: List<TextView>
    private lateinit var tabs: List<LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<TextView>(R.id.btn_about).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        pages = listOf(
            findViewById(R.id.page_discover),
            findViewById(R.id.page_wallpapers),
            findViewById(R.id.page_icons),
            findViewById(R.id.page_widgets)
        )
        dots = listOf(
            findViewById(R.id.dot_discover),
            findViewById(R.id.dot_wallpapers),
            findViewById(R.id.dot_icons),
            findViewById(R.id.dot_widgets)
        )
        labels = listOf(
            findViewById(R.id.label_discover),
            findViewById(R.id.label_wallpapers),
            findViewById(R.id.label_icons),
            findViewById(R.id.label_widgets)
        )
        tabs = listOf(
            findViewById(R.id.tab_discover),
            findViewById(R.id.tab_wallpapers),
            findViewById(R.id.tab_icons),
            findViewById(R.id.tab_widgets)
        )
        tabs.forEachIndexed { i, t -> t.setOnClickListener { selectTab(i) } }

        populate(findViewById(R.id.discover_list), BundleRepository.bundles)
        populate(findViewById(R.id.wallpapers_list), BundleRepository.bundles)
        populate(findViewById(R.id.icons_list), BundleRepository.bundles)

        selectTab(0)
    }

    override fun onResume() {
        super.onResume()
        // refresh lock states in case something changed
        populate(findViewById(R.id.discover_list), BundleRepository.bundles)
        populate(findViewById(R.id.wallpapers_list), BundleRepository.bundles)
        populate(findViewById(R.id.icons_list), BundleRepository.bundles)
    }

    private fun selectTab(i: Int) {
        pages.forEachIndexed { idx, v -> v.visibility = if (idx == i) View.VISIBLE else View.GONE }
        dots.forEachIndexed { idx, d -> d.visibility = if (idx == i) View.VISIBLE else View.INVISIBLE }
        val text = getColor(R.color.text)
        val muted = getColor(R.color.muted)
        labels.forEachIndexed { idx, l -> l.setTextColor(if (idx == i) text else muted) }
    }

    private fun populate(container: LinearLayout, bundles: List<RizBundle>) {
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (b in bundles) {
            val card = inflater.inflate(R.layout.item_bundle, container, false)
            card.findViewById<TextView>(R.id.card_name).text = b.name
            card.findViewById<TextView>(R.id.card_tagline).text = b.tagline
            card.findViewById<TextView>(R.id.card_counts).text =
                "${b.wallpaperCount} wallpapers • ${b.iconCount} icons"
            val accent = card.findViewById<View>(R.id.card_accent)
            (accent.background as? GradientDrawable)?.setColor(b.accentColor)
                ?: accent.setBackgroundColor(b.accentColor)

            val unlocked = UnlockManager.isUnlocked(this, b.id)
            val action = card.findViewById<TextView>(R.id.card_action)
            action.text = if (unlocked) "Open" else "🔒 Unlock"
            action.setOnClickListener {
                if (UnlockManager.isUnlocked(this, b.id)) {
                    Toast.makeText(this, "Bundle content arrives in the next drop ✨", Toast.LENGTH_SHORT).show()
                } else {
                    showUnlockDialog(b)
                }
            }
            container.addView(card)
        }
    }

    private fun showUnlockDialog(b: RizBundle) {
        AlertDialog.Builder(this)
            .setTitle("Unlock ${b.name}")
            .setMessage(
                "Watch a short ad to unlock ${b.wallpaperCount} wallpapers + ${b.iconCount} icons.\n\n" +
                "Rewarded ads plug in here soon (AdMob). Dev shortcut below."
            )
            .setPositiveButton("Unlock now (dev)") { d, _ ->
                UnlockManager.unlock(this, b.id)
                d.dismiss()
                onResume()
                Toast.makeText(this, "${b.name} unlocked — enjoy the glow ✨", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Later", null)
            .show()
    }
}
