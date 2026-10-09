package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NonFiniteSaveTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun nonFiniteValuesLeaveProjectFileAndHistoryUnchanged() {
        val storage = DesktopStorageManager(folder.newFolder())
        val state = DesktopAppState(storage)
        val device = Device(technicalName = "Device", mountingType = MountingType.OUT_OF_RACK)
        val initial = Project(name = "Finite project", createdEpochMs = 1, updatedEpochMs = 1,
            sites = listOf(Site(name = "Site", devices = listOf(device))))
        try {
            state.importFile(storage.saveProjectLocally(initial), compare = false)
            val before = state.project!!
            val file = storage.listStoredProjects().single().file
            val bytes = file.readBytes()
            for (number in listOf(Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY)) {
                state.update(before.copy(powerFeeds = listOf(PowerFeed(deviceId = device.id, feedName = "Invalid", loadWatts = number))), "Invalid")
                assertNotNull(state.error)
                assertEquals(before, state.project)
                assertFalse(state.canUndo)
                assertArrayEquals(bytes, file.readBytes())
            }
        } finally { state.shutdown() }
    }
}
