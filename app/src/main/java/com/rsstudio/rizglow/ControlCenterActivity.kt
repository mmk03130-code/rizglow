package com.rsstudio.rizglow

import android.app.Activity
import android.os.Bundle

/** In-app entry to the panel (Discover card, bubble, QS tile). Thin wrapper
 *  around the shared [ControlCenterPanelView]. The swipe path shows the same
 *  view as a system overlay instead, so it feels instant like the big apps. */
class ControlCenterActivity : Activity() {

    private var panel: ControlCenterPanelView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        overridePendingTransition(0, 0) // no app-launch animation; feels like an overlay
        panel = ControlCenterPanelView(this, onDismiss = { finish() })
        setContentView(panel)
        panel?.post { panel?.playEntrance() }
    }

    override fun onStart() {
        super.onStart()
        ControlCenterGate.panelOpen = true
    }

    override fun onResume() {
        super.onResume()
        panel?.refresh()
    }

    override fun onStop() {
        ControlCenterGate.panelOpen = false
        super.onStop()
    }

    override fun finish() {
        super.finish()
        overridePendingTransition(0, 0)
    }
}
