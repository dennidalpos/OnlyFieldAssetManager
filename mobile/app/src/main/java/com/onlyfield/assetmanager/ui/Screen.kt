package com.onlyfield.assetmanager.ui

/** Destinations of the app. The back stack is kept in [ProjectViewModel]. */
sealed interface Screen {
    data object Projects : Screen
    data object NewSite : Screen
    data object Home : Screen
    data object Inventory : Screen
    data class DeviceDetail(val deviceId: String) : Screen
    data object Structure : Screen
    data object Racks : Screen
    data class RackDetail(val rackId: String) : Screen
    data object Models : Screen
    data object Cabling : Screen
    data object Network : Screen
    data object Power : Screen
    data object Attachments : Screen
    data object Floorplan : Screen
    data object Credentials : Screen
    data object Trash : Screen
    data object Documents : Screen
    data object Issues : Screen
}
