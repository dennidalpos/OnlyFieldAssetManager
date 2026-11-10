package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

/** One block of the device gallery: a rack with what it holds, another container, or the loose devices ([container] null). */
data class GalleryGroup(val container: ObjectRef?, val rack: Rack?, val devices: List<Device>)

/** Devices of a site, floor or container grouped as they are found on site: racks first, then other containers, then the rest. */
object DeviceGalleries {
    /** [endpoints] off leaves out PCs, phones, cameras and the like, as in the topology. */
    fun build(project: Project, siteId: String? = null, areaId: String? = null, container: ObjectRef? = null, endpoints: Boolean = false,
              hierarchy: HierarchyIndex = HierarchyIndex(project)): List<GalleryGroup> {
        val siteAreas = project.sites.filter { siteId == null || it.id == siteId }.flatMap { it.areas }.map { it.id }.toSet()
        val inside = container?.let { hierarchy.descendants(it).toSet() + it }
        fun shown(ref: ObjectRef) = (inside == null || ref in inside) && (areaId == null || hierarchy.areaId(ref) == areaId)
        val racks = project.racks.filter { r -> shown(ObjectRef(PlacementTargetType.RACK, r.id)) && (siteId == null || r.areaId in siteAreas) }
        val devices = project.sites.filter { siteId == null || it.id == siteId }.flatMap { it.devices }
            .filter { shown(ObjectRef(PlacementTargetType.DEVICE, it.id)) && (endpoints || ObjectGlyph.of(project, it).family != ObjectFamily.ENDPOINT) }
        val byParent = devices.groupBy { hierarchy.parents[ObjectRef(PlacementTargetType.DEVICE, it.id)] }
        val rackRefs = racks.associateBy { ObjectRef(PlacementTargetType.RACK, it.id) }
        // Top unit first, as the rack is read; devices without a unit follow by name.
        val inRacks = racks.sortedBy { it.name.lowercase() }.map { rack ->
            val ref = ObjectRef(PlacementTargetType.RACK, rack.id)
            GalleryGroup(ref, rack, byParent[ref].orEmpty().sortedWith(compareBy<Device>({ it.positionU == null }, { -(it.positionU ?: 0) }, { it.technicalName.lowercase() })))
        }
        val inContainers = byParent.filterKeys { it != null && it !in rackRefs }.map { (ref, list) -> GalleryGroup(ref, null, list.sortedBy { it.technicalName.lowercase() }) }
            .sortedBy { ObjectHierarchy.name(project, it.container!!).lowercase() }
        val loose = byParent[null].orEmpty().sortedBy { it.technicalName.lowercase() }
        return inRacks + inContainers + listOfNotNull(GalleryGroup(null, null, loose).takeIf { loose.isNotEmpty() })
    }
}
