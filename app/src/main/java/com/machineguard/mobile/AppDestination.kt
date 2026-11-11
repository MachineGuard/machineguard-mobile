package com.machineguard.mobile

import androidx.annotation.StringRes

/** Destinos de la navegación inferior (Capítulo V, Mobile Application). */
enum class AppDestination(@StringRes val labelRes: Int) {
    Dashboard(R.string.nav_dashboard),
    Alerts(R.string.nav_alerts),
    Settings(R.string.nav_settings),
}
