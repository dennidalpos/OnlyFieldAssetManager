package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.model.*

/**
 * One object along a physical path. A pass-through lists the port where the path enters and,
 * after its internal passage, the port where it leaves; an end lists one port (none when the cable
 * stops at a device without a port).
 */
data class PathStation(val device: Device?, val ports: List<Port>) {
    /** Unknown or passive: as an end, the path stops here without reaching an active device. */
    val passive: Boolean get() = device == null || device.isPassive()
}

/** Cable between [PathSchematic.stations] i and i + 1, with the label to write on it. */
data class PathSegment(val cable: Cable, val label: String)

/**
 * End-to-end physical path, drawn by the port card, the port page and the PDF.
 * [segments] has one element fewer than [stations]; [focus] is the station of the queried port.
 */
data class PathSchematic(val stations: List<PathStation>, val segments: List<PathSegment>, val focus: Int, val state: ConnectionState) {
    /** Station [i] is an end that does not reach an active device. */
    fun openEnd(i: Int): Boolean = (i == 0 || i == stations.lastIndex) && stations[i].passive
}

object PathSchematics {
    /** Null for unknown ports; a free port gives a single station. */
    fun of(project: Project, portId: String, graph: ConnectionGraph = ConnectionGraph(project), index: ProjectIndex = ProjectIndex(project)): PathSchematic? {
        val ref = index.port(portId) ?: return null
        val walker = Walker(project, index)
        val visited = mutableSetOf(portId)
        // A pass-through is crossed in both directions: the side behind its internal passage first.
        val partner = if (ref.device.isPassive()) walker.partner(portId)?.takeIf { visited.add(it.id) } else null
        val back = partner?.let { walker.walk(it, visited) } ?: Leg(emptyList(), emptyList())
        val ahead = walker.walk(ref.port, visited)
        var stations = back.stations.reversed() + PathStation(ref.device, listOfNotNull(partner, ref.port)) + ahead.stations
        var segments = back.segments.reversed() + ahead.segments
        var focus = back.stations.size
        // Read from the active end when only one side has it (switch → … → outlet).
        if (stations.first().passive && !stations.last().passive) {
            stations = stations.reversed().map { it.copy(ports = it.ports.reversed()) }
            segments = segments.reversed()
            focus = stations.size - 1 - focus
        }
        return PathSchematic(stations, segments, focus, graph.state(portId))
    }

    private class Leg(val stations: List<PathStation>, val segments: List<PathSegment>)

    private class Walker(private val project: Project, private val index: ProjectIndex) {
        private val cables = PhotoCoverage.cablesByPort(project)
        private val mappings = buildMap {
            project.panelMappings.filter { !it.isUnknownPassage }.forEach { m -> m.portBId?.let { b -> put(m.portAId, b); put(b, m.portAId) } }
        }

        fun partner(portId: String): Port? = mappings[portId]?.let { index.port(it)?.port }

        /** Stations reached through the cable of [start], following internal passages until an end. */
        fun walk(start: Port, visited: MutableSet<String>): Leg {
            val stations = mutableListOf<PathStation>()
            val segments = mutableListOf<PathSegment>()
            var port = start
            while (true) {
                val cable = cables[port.id] ?: break
                segments += PathSegment(cable, cable.codeOrLabel ?: CableLabels.suggest(project, cable, index))
                val farPortId = if (cable.portAId == port.id) cable.portBId else cable.portAId
                val far = farPortId?.let(index::port)
                if (far == null) {
                    stations += PathStation(index.device(if (cable.portAId == port.id) cable.deviceBId else cable.deviceAId), emptyList())
                    break
                }
                visited += far.port.id
                // A cycle or an unknown passage ends the drawing at this object.
                val next = if (far.device.isPassive()) partner(far.port.id)?.takeIf { visited.add(it.id) } else null
                stations += PathStation(far.device, listOfNotNull(far.port, next))
                port = next ?: break
            }
            return Leg(stations, segments)
        }
    }
}
