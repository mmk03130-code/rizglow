package com.rsstudio.rizglow

import android.content.Intent
import android.service.quicksettings.TileService

/** Quick Settings tile: tap to float the Rizglow control center over anything. */
class ControlCenterTileService : TileService() {
    override fun onClick() {
        super.onClick()
        startActivityAndCollapse(Intent(this, ControlCenterActivity::class.java))
    }
}
