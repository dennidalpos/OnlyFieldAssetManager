package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.core.model.CableMedium
import com.onlyfield.assetmanager.core.model.ConnectionGraph
import com.onlyfield.assetmanager.core.model.Port
import com.onlyfield.assetmanager.core.model.Project

/** Series cabling: port N of one device to port M of another, then N+1 to M+1 and so on. */
object BulkCabling {

    /** Free ports from [startPortId] onwards, same device and side, in technical order. */
    fun freeRun(project: Project, startPortId: String, graph: ConnectionGraph = ConnectionGraph(project)): List<Port> {
        val device = project.sites.flatMap { it.devices }.firstOrNull { d -> d.ports.any { it.id == startPortId } } ?: return emptyList()
        val start = device.ports.indexOfFirst { it.id == startPortId }
        val side = device.ports[start].hardware.side
        return device.ports.drop(start).filter { it.hardware.side == side && !graph.occupied(it.id) }
    }

    /** Pairs to cable, at most [count]; both runs start at the given ports. */
    fun pairs(project: Project, fromPortId: String, toPortId: String, count: Int, graph: ConnectionGraph = ConnectionGraph(project)): List<Pair<Port, Port>> =
        freeRun(project, fromPortId, graph).zip(freeRun(project, toPortId, graph)).take(count.coerceAtLeast(0))

    fun maxCount(project: Project, fromPortId: String, toPortId: String, graph: ConnectionGraph = ConnectionGraph(project)): Int =
        minOf(freeRun(project, fromPortId, graph).size, freeRun(project, toPortId, graph).size)

    /** Connects every pair with [medium] and the suggested `A/P – B/P` label. */
    fun connect(project: Project, pairs: List<Pair<Port, Port>>, medium: CableMedium): Project = pairs.fold(project) { p, (a, b) ->
        val connected = HardwareConfigurator.connect(p, a.id, b.id, medium)
        val cable = connected.cables.first { setOf(it.portAId, it.portBId) == setOf(a.id, b.id) }
        ProjectEdits.updateCable(connected, cable.copy(codeOrLabel = CableLabels.suggest(connected, cable, ProjectIndex(connected))))
    }

    /** Next free port after [portId] on the same device and side, for continuous cabling. */
    fun nextFree(project: Project, portId: String, graph: ConnectionGraph = ConnectionGraph(project)): String? =
        freeRun(project, portId, graph).firstOrNull { it.id != portId }?.id
}
