package com.vr_object.fixed.xnzrw24b.data

/**
 * Created by Michael Lukin on 06.12.2017.
 */
object GlobalSettings {
    enum class WorkMode {
        WIFI,
        BLE
    }
    @JvmStatic var mode: WorkMode = GlobalSettings.WorkMode.WIFI
}