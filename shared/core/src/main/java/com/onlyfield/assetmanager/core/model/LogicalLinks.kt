package com.onlyfield.assetmanager.core.model

/** WAN/VPN/Internet links ending on a device: anchored to the device, never to a port. */
object LogicalLinks {
    fun of(project: Project, deviceId: String): List<WanVpnConnection> =
        project.wanVpnConnections.filter { it.localEndpointDeviceId == deviceId || it.remoteEndpointDeviceId == deviceId }

    /** Far side seen from [deviceId]: device id and site description. */
    fun far(link: WanVpnConnection, deviceId: String): Pair<String?, String?> =
        if (link.localEndpointDeviceId == deviceId) link.remoteEndpointDeviceId to link.remoteEndpointSiteDescription
        else link.localEndpointDeviceId to link.localEndpointSiteDescription

    /** "FW-02 · Primo · Sede B"; null when the far side is not described. */
    fun farLabel(project: Project, link: WanVpnConnection, deviceId: String): String? {
        val (id, site) = far(link, deviceId)
        val device = id?.let { d -> project.sites.flatMap { it.devices }.find { it.id == d } }
        val area = device?.let { ObjectHierarchy.areaId(project, ObjectRef(PlacementTargetType.DEVICE, it.id)) }
            ?.let { a -> project.sites.flatMap { it.areas }.find { it.id == a }?.name }
        return listOfNotNull(device?.technicalName, area, site?.takeIf { it.isNotBlank() }).joinToString(" · ").ifBlank { null }
    }
}
