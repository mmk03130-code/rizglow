package com.rsworks.rizglow

import android.content.Context
import android.content.pm.PackageManager

/** Detects third-party launchers that support icon packs, so the Icons tab
 *  can give tailored apply guidance (the CandyBar pattern). */
object LauncherDetector {

    /** Package → display name for launchers with icon-pack support. */
    private val known = mapOf(
        "app.lawnchair" to "Lawnchair",
        "com.teslacoilsw.launcher" to "Nova Launcher",
        "com.actionlauncher.playstore" to "Action Launcher",
        "bitpit.launcher" to "Niagara Launcher",
        "ginlemon.flowerfree" to "Smart Launcher",
        "ginlemon.flowerpro" to "Smart Launcher Pro",
        "com.hyperion.launcher" to "Hyperion",
        "com.anddoes.launcher" to "Apex Launcher",
        "com.mi.android.globallauncher" to "POCO Launcher",
        "com.samsung.android.themestore" to "Samsung Theme Store"
    )

    /** Names of supported launchers installed on this phone. */
    fun installed(ctx: Context): List<String> {
        val pm = ctx.packageManager
        return known.mapNotNull { (pkg, name) ->
            try {
                pm.getPackageInfo(pkg, 0)
                name
            } catch (e: PackageManager.NameNotFoundException) {
                null
            }
        }
    }
}
