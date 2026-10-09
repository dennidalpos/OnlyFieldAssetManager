package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.*
import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.zip.ZipInputStream

class PowerDocumentTest {
    private val target = Device(technicalName = "Selected consumer")
    private val sources = listOf(Device(technicalName = "Source UPS"), Device(technicalName = "Source PDU"))
    private val selectedSite = Site(name = "Selected", devices = listOf(target))
    private val otherSite = Site(name = "External", devices = sources)
    private val feeds = PowerFeedType.entries.mapIndexed { n, type -> PowerFeed(deviceId = target.id,
        feedName = "Custom circuit $n", feedType = type, sourceDeviceId = sources[n % 2].id,
        sourceOutletDescription = "Outlet $n", voltageVolts = 230, loadVa = 80.0 + n, loadWatts = 60.0 + n,
        observedRuntimeMinutes = 10 + n, observedSource = "Meter $n", observedEpochMs = 1700000000000L,
        notes = "Feed note $n") }
    private val project = Project(name = "Power", createdEpochMs = 0, updatedEpochMs = 0,
        sites = listOf(selectedSite, otherSite), powerFeeds = feeds + PowerFeed(deviceId = sources.first().id,
            feedName = "EXCLUDED-FEED", sourceOutletDescription = "EXCLUDED-OUTLET"))

    @Test fun markdownListsEveryCustomFeedWithAllRecordedFieldsInThreeLanguages() {
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language))
            val output = ByteArrayOutputStream()
            MarkdownExportManager.exportMarkdownToStream(project, ExportFilterConfig(selectedSiteId = selectedSite.id), output, i18n)
            val text = output.toString("UTF-8")
            for (feed in feeds) {
                val row = text.lines().singleOrNull { it.startsWith("|") && it.contains(feed.feedName) }
                assertNotNull("$language missing feed ${feed.feedName}", row)
                for (value in listOf(feed.feedType.toDisplayString(i18n), sources.first { it.id == feed.sourceDeviceId }.technicalName,
                    feed.sourceOutletDescription!!, "230", feed.loadVa.toString(), feed.loadWatts.toString(),
                    feed.observedRuntimeMinutes.toString(), feed.observedSource!!, feed.notes!!)) assertTrue("$language missing $value", row!!.contains(value.replace("(", "\\(").replace(")", "\\)")))
            }
            assertFalse(text.contains("EXCLUDED-FEED")); assertFalse(text.contains("EXCLUDED-OUTLET"))
        }
    }

    @Test fun xlsxKeepsAllCustomFeedFieldsAndNumericLoadsInThreeLanguages() {
        for (language in listOf("it", "en", "es")) {
            val i18n = Messages(Locale.forLanguageTag(language)); val output = ByteArrayOutputStream()
            XlsxExportManager.exportXlsxToStream(project, ExportFilterConfig(selectedSiteId = selectedSite.id), output, i18n)
            val sheet = ZipInputStream(output.toByteArray().inputStream()).use { zip ->
                generateSequence { zip.nextEntry }.first { it.name == "xl/worksheets/sheet4.xml" }; zip.readBytes().toString(Charsets.UTF_8)
            }
            feeds.forEach { feed -> for (value in listOf(feed.feedName, feed.feedType.toDisplayString(i18n),
                sources.first { it.id == feed.sourceDeviceId }.technicalName, feed.sourceOutletDescription!!,
                feed.observedSource!!, feed.notes!!, "<v>${feed.loadVa}</v>", "<v>${feed.loadWatts}</v>")) assertTrue("$language missing $value", sheet.contains(value)) }
            assertFalse(sheet.contains("EXCLUDED-FEED")); assertFalse(sheet.contains("EXCLUDED-OUTLET"))
        }
    }
}
