package com.onlyfield.assetmanager.core.model

import com.onlyfield.assetmanager.core.i18n.Messages

import kotlinx.serialization.Serializable

@Serializable
data class ObjectRef(val type: PlacementTargetType, val id: String)

/** Identity follows the child, so moving it produces one merge conflict. */
@Serializable
data class ObjectContainment(val child: ObjectRef, val parent: ObjectRef, val id: String = child.id)

@Serializable
data class MountSnapshot(val deviceId: String, val rackId: String?, val positionU: Int?, val side: RackSide, val mounting: MountingType)

/** Precomputed hierarchy lookups for views that resolve many refs at once. */
class HierarchyIndex(project: Project) {
    private val devices = project.businessUnits.flatMap { it.devices }.associateBy { it.id }
    private val racks = project.racks.associateBy { it.id }
    val parents: Map<ObjectRef, ObjectRef> = ObjectHierarchy.relations(project).associate { it.child to it.parent }
    val children: Map<ObjectRef, List<ObjectRef>> = parents.entries.groupBy({ it.value }, { it.key })

    fun ancestors(ref: ObjectRef): List<ObjectRef> {
        val seen = mutableSetOf(ref)
        return generateSequence(parents[ref]) { parents[it] }.takeWhile { seen.add(it) }.toList()
    }
    fun root(ref: ObjectRef): ObjectRef = ancestors(ref).lastOrNull() ?: ref
    fun areaId(ref: ObjectRef): String? = root(ref).let { r ->
        if (r.type == PlacementTargetType.RACK) racks[r.id]?.areaId else devices[r.id]?.areaId
    }
    fun descendants(ref: ObjectRef): List<ObjectRef> {
        val seen = mutableSetOf(ref)
        val result = mutableListOf<ObjectRef>()
        val queue = ArrayDeque(children[ref].orEmpty())
        while (queue.isNotEmpty()) {
            val next = queue.removeFirst()
            if (seen.add(next)) { result += next; queue += children[next].orEmpty() }
        }
        return result
    }
}

object ObjectHierarchy {
    fun refs(project: Project): List<ObjectRef> = project.racks.map { ObjectRef(PlacementTargetType.RACK, it.id) } +
        project.businessUnits.flatMap { it.devices }.map { ObjectRef(PlacementTargetType.DEVICE, it.id) }

    fun name(project: Project, ref: ObjectRef, i18n: Messages = Messages()): String = when (ref.type) {
        PlacementTargetType.RACK -> project.racks.find { it.id == ref.id }?.name
        PlacementTargetType.DEVICE -> project.businessUnits.flatMap { it.devices }.find { it.id == ref.id }?.technicalName
    } ?: i18n.text("text.959a2f5a08e6")

    fun canContain(project: Project, ref: ObjectRef): Boolean = when (ref.type) {
        PlacementTargetType.RACK -> project.racks.any { it.id == ref.id }
        PlacementTargetType.DEVICE -> project.businessUnits.flatMap { it.devices }.find { it.id == ref.id }
            ?.let { ObjectCatalog.type(project, it.objectTypeId)?.let { type -> type.kind == ObjectKind.DEVICE && type.canContainObjects } } == true
    }

    fun relations(project: Project): List<ObjectContainment> {
        val explicit = project.objectContainments
        val children = explicit.map { it.child }.toSet()
        return explicit + project.businessUnits.flatMap { it.devices }.mapNotNull { d ->
            val rack = project.racks.find { it.id == d.rackId } ?: return@mapNotNull null
            val child = ObjectRef(PlacementTargetType.DEVICE, d.id)
            if (child in children) null else ObjectContainment(child, ObjectRef(PlacementTargetType.RACK, rack.id))
        }
    }

    fun normalize(project: Project): Project = project.copy(objectContainments = relations(project))
    fun synchronize(project: Project): Project = synchronize(normalize(project), project)
    fun parent(project: Project, ref: ObjectRef): ObjectRef? = relations(project).find { it.child == ref }?.parent
    fun children(project: Project, ref: ObjectRef): List<ObjectRef> = relations(project).filter { it.parent == ref }.map { it.child }

    fun ancestors(project: Project, ref: ObjectRef): List<ObjectRef> {
        val parents = relations(project).associate { it.child to it.parent }
        val seen = mutableSetOf(ref)
        val result = mutableListOf<ObjectRef>()
        var current = parents[ref]
        while (current != null && seen.add(current)) {
            result += current
            current = parents[current]
        }
        return result
    }

    fun root(project: Project, ref: ObjectRef): ObjectRef = ancestors(project, ref).lastOrNull() ?: ref
    fun areaId(project: Project, ref: ObjectRef): String? {
        val root = root(project, ref)
        return when (root.type) {
            PlacementTargetType.RACK -> project.racks.find { it.id == root.id }?.areaId
            PlacementTargetType.DEVICE -> project.businessUnits.flatMap { it.devices }.find { it.id == root.id }?.areaId
        }
    }

    fun errors(project: Project, i18n: Messages = Messages()): List<String> = buildList {
        val refs = refs(project).toSet()
        val entries = relations(project)
        if (entries.map { it.child }.distinct().size != entries.size) add(i18n.text("text.9b8bfd31aaaa"))
        entries.forEach { entry ->
            if (entry.child !in refs || entry.parent !in refs) add(i18n.text("text.027942ebbf8b"))
            if (entry.id != entry.child.id) add(i18n.text("text.d52820b82761"))
            if (!canContain(project, entry.parent)) add(i18n.text("text.161bcdb0c3e4"))
            if (entry.child == entry.parent || ancestors(project, entry.parent).contains(entry.child)) add(i18n.text("text.a436233ba737"))
        }
    }.distinct()

    fun assign(project: Project, child: ObjectRef, parent: ObjectRef?, i18n: Messages = Messages()): Project {
        require(child in refs(project)) { i18n.text("text.b4f0135a1956") }
        val before = normalize(project)
        val floor = areaId(before, child)
        val updated = before.copy(objectContainments = before.objectContainments.filterNot { it.child == child } +
            if (parent == null) emptyList() else listOf(ObjectContainment(child, parent)))
        // Clear the legacy mounting link before resolving the new hierarchy.
        val detached = updated.copy(businessUnits = updated.businessUnits.map { bu -> bu.copy(devices = bu.devices.map { d ->
            if (child.type == PlacementTargetType.DEVICE && d.id == child.id) d.copy(rackId = null, areaId = if (parent == null) floor else d.areaId) else d
        }) }, racks = updated.racks.map { r ->
            if (child.type == PlacementTargetType.RACK && r.id == child.id && parent == null) r.copy(areaId = floor) else r
        })
        require(errors(detached, i18n = i18n).isEmpty()) { errors(detached, i18n = i18n).joinToString("; ") }
        return synchronize(detached, before).copy(updatedEpochMs = System.currentTimeMillis())
    }

    private fun synchronize(project: Project, before: Project): Project {
        val devicesBefore = before.businessUnits.flatMap { it.devices }.associateBy { it.id }
        return project.copy(
            businessUnits = project.businessUnits.map { bu -> bu.copy(devices = bu.devices.map { d ->
                val ref = ObjectRef(PlacementTargetType.DEVICE, d.id)
                val rackId = ancestors(project, ref).firstOrNull { it.type == PlacementTargetType.RACK }?.id
                val old = devicesBefore[d.id]
                val sameRack = old?.rackId == rackId
                d.copy(rackId = rackId, areaId = if (parent(project, ref) != null) areaId(project, ref) else d.areaId,
                    positionU = if (sameRack) d.positionU else null,
                    mountingType = if (sameRack) d.mountingType else if (rackId == null) MountingType.OUT_OF_RACK else MountingType.RACK_MOUNT)
            }) },
            racks = project.racks.map { r ->
                val ref = ObjectRef(PlacementTargetType.RACK, r.id)
                if (parent(project, ref) == null) r else r.copy(areaId = areaId(project, ref))
            },
            updatedEpochMs = project.updatedEpochMs
        )
    }

    /** Release direct children; deeper descendants retain their parent. */
    fun release(project: Project, ref: ObjectRef): Project {
        val before = normalize(project)
        val parent = parent(before, ref)
        val floor = areaId(before, ref)
        val children = children(before, ref).toSet()
        val result = before.copy(objectContainments = before.objectContainments.filterNot { it.child == ref || it.parent == ref } +
            if (parent == null) emptyList() else children.map { ObjectContainment(it, parent) },
            businessUnits = before.businessUnits.map { bu -> bu.copy(devices = bu.devices.map { d ->
                if (ObjectRef(PlacementTargetType.DEVICE, d.id) in children) d.copy(rackId = null, areaId = floor) else d
            }) },
            racks = before.racks.map { r -> if (ObjectRef(PlacementTargetType.RACK, r.id) in children) r.copy(areaId = floor) else r })
        return synchronize(result, before)
    }

    fun restore(project: Project, trash: TrashItem, i18n: Messages = Messages()): Project {
        var result = normalize(project)
        val available = refs(result).toSet()
        for (entry in trash.containments) {
            if (entry.child !in available || entry.parent !in available) continue
            result = assign(result, entry.child, entry.parent, i18n = i18n)
        }
        return result.copy(businessUnits = result.businessUnits.map { bu -> bu.copy(devices = bu.devices.map { d ->
            trash.mountSnapshots.find { it.deviceId == d.id && it.rackId == d.rackId }?.let {
                d.copy(positionU = it.positionU, rackSide = it.side, mountingType = it.mounting)
            } ?: d
        }) }, floorplanPlacements = result.floorplanPlacements + trash.containmentPlacements.filter { p ->
            result.floorplanPlacements.none { it.id == p.id }
        })
    }

    fun snapshot(project: Project, ref: ObjectRef, trash: TrashItem): TrashItem = trash.copy(
        containments = relations(project).filter { it.child == ref || it.parent == ref },
        mountSnapshots = project.businessUnits.flatMap { it.devices }.filter {
            val device = ObjectRef(PlacementTargetType.DEVICE, it.id)
            device == ref || ref in ancestors(project, device)
        }.map { MountSnapshot(it.id, it.rackId, it.positionU, it.rackSide, it.mountingType) },
        containmentPlacements = project.floorplanPlacements.filter { it.targetId == ref.id && it.targetType == ref.type },
        originalBusinessUnitId = project.businessUnits.find { b -> b.devices.any { it.id == ref.id } }?.id
    )

    fun afterDeletion(before: Project, after: Project, ref: ObjectRef): Project {
        val released = release(before, ref)
        val mounts = released.businessUnits.flatMap { it.devices }.associateBy { it.id }
        return after.copy(objectContainments = released.objectContainments,
            businessUnits = after.businessUnits.map { bu -> bu.copy(devices = bu.devices.map { d ->
                mounts[d.id]?.let { d.copy(rackId = it.rackId, positionU = it.positionU, mountingType = it.mountingType, areaId = it.areaId) } ?: d
            }) },
            racks = after.racks.map { r -> released.racks.find { it.id == r.id } ?: r })
    }
}
