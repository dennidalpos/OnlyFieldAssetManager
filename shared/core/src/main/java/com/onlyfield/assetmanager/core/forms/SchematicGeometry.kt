package com.onlyfield.assetmanager.core.forms

import com.onlyfield.assetmanager.core.model.*

data class PortRow(val group: String?, val ports: List<Port>)

/** Port cells fitted to a width: [bands] of rows of port indices, every cell [cell] wide. */
data class PortGrid(val columns: Int, val cell: Float, val bands: List<List<List<Int>>>)

object SchematicGeometry {
    fun arrangedGrid(count: Int, rows: Int, width: Float, gap: Float, cellSize: Float): PortGrid {
        if (count == 0) return PortGrid(0, cellSize, emptyList())
        val natural = (count + rows - 1) / rows
        val columns = ((width + gap) / (cellSize + gap)).toInt().coerceIn(1, natural)
        val logicalRows = (0 until count).toList().chunked(natural)
        val bands = (0 until natural step columns).map { start -> logicalRows.map { it.drop(start).take(columns) }.filter { it.isNotEmpty() } }
        return PortGrid(columns, cellSize, bands)
    }

    fun rows(device: Device, side: PortSide): List<PortRow> = device.ports
        .filter { it.hardware.side == null || it.hardware.side == side || it.hardware.side == PortSide.BOTH }
        .groupBy { it.hardware.group }.flatMap { (group, ports) -> ports.chunked(8).map { PortRow(group, it) } }

    fun rackUnits(rack: Rack): List<Int> = if (rack.numberingDirection == NumberingDirection.BOTTOM_TO_TOP)
        (rack.heightU downTo 1).toList() else (1..rack.heightU).toList()

    /**
     * Fits [count] ports into [width] (any unit) without horizontal scrolling. Blocks over 8 ports
     * keep the switch layout, odd above and even below; when they do not fit they wrap into
     * balanced bands (48 ports at phone width: three bands of 2 × 8).
     */
    fun portGrid(count: Int, width: Float, gap: Float, minCell: Float, maxCell: Float): PortGrid {
        if (count <= 0) return PortGrid(0, maxCell, emptyList())
        val paired = count > 8
        val natural = if (paired) (count + 1) / 2 else count
        val fit = ((width + gap) / (minCell + gap)).toInt().coerceAtLeast(1)
        val bandsCount = (natural + fit - 1) / fit
        val columns = (natural + bandsCount - 1) / bandsCount
        val cell = ((width - gap * (columns - 1)) / columns).coerceIn(minCell, maxCell)
        val indices = (0 until count).toList()
        val bands = if (paired) indices.chunked(columns * 2).map { band -> listOf(band.filterIndexed { i, _ -> i % 2 == 0 }, band.filterIndexed { i, _ -> i % 2 == 1 }) }
        else indices.chunked(columns).map { listOf(it) }
        return PortGrid(columns, cell, bands)
    }
}
