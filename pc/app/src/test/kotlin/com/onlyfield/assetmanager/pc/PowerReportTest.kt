package com.onlyfield.assetmanager.pc

import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Locale

class PowerReportTest {
    @Test fun pdfIncludesEveryCustomFeedFieldWithFilteredUpstreamContext() {
        val target = Device(technicalName = "Consumer")
        val source = Device(technicalName = "External source")
        val site = Site(name = "Selected", devices = listOf(target))
        val feeds = PowerFeedType.entries.mapIndexed { n, type -> PowerFeed(deviceId = target.id,
            feedName = "Circuit-$n", feedType = type, sourceDeviceId = source.id, sourceOutletDescription = "Outlet-$n",
            voltageVolts = 230, loadVa = 80.0 + n, loadWatts = 60.0 + n, observedRuntimeMinutes = 10 + n,
            observedSource = "Meter-$n", observedEpochMs = 1700000000000L, notes = "Note-$n") }
        val project = Project(name = "Power", createdEpochMs = 0, updatedEpochMs = 0,
            sites = listOf(site, Site(name = "Other", devices = listOf(source))),
            powerFeeds = feeds + PowerFeed(deviceId = source.id, feedName = "EXCLUDED-FEED"))
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val bytes = ByteArrayOutputStream().also { DesktopDocumentManager.exportCompositePdf(project,
                ExportFilterConfig(selectedSiteId = site.id), ReportSelection(includeInventoryTable = false,
                    includeNotesAndAttachments = false, includeFloorPlans = false, includeTopology = false, includePaths = false), it, i18n) }.toByteArray()
            val text = Loader.loadPDF(bytes).use { PDFTextStripper().getText(it) }.filterNot(Char::isWhitespace)
            for (feed in feeds) for (value in listOf(feed.feedName, feed.feedType.toDisplayString(i18n), source.technicalName,
                feed.sourceOutletDescription!!, "230 V", "${feed.loadVa} VA", "${feed.loadWatts} W",
                i18n.text("text.2dc280aa0f83", feed.observedRuntimeMinutes!!), feed.observedSource!!, feed.notes!!, "2023"))
                assertTrue("$language missing $value", text.contains(value.filterNot(Char::isWhitespace)))
            assertFalse(text.contains("EXCLUDED-FEED"))
        }
    }
}
