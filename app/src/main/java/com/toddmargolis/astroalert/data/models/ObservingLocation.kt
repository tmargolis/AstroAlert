package com.toddmargolis.astroalert.data.models

data class ObservingLocation(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val enabled: Boolean = true
) {
    companion object {
        val INDIANA_DUNES = ObservingLocation("Indiana Dunes", 41.66, -87.04, enabled = true)
        val HEBRON = ObservingLocation("Hebron, IL", 42.47, -88.43, enabled = false)
        val ZION = ObservingLocation("Zion, IL", 42.45, -87.85, enabled = false)
        val AFTON_FOREST = ObservingLocation("Afton Forest Preserve", 41.83, -88.73, enabled = false)

        val ALL_LOCATIONS = listOf(INDIANA_DUNES, HEBRON, ZION, AFTON_FOREST)
        val ENABLED_LOCATIONS = ALL_LOCATIONS.filter { it.enabled }
    }
}