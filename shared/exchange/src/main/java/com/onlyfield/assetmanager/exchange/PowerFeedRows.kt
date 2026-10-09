package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.i18n.Messages
import com.onlyfield.assetmanager.core.model.PowerFeed
import java.text.SimpleDateFormat
import java.util.Date

/** One row per feed; upstream labels come from the original project, including external sources. */
internal object PowerFeedRows {
    fun headers(i18n: Messages) = listOf("text.cf301d95d32c", "document.feedName", "text.3868d2843d59",
        "text.11ae92f057eb", "text.cd33696ca977", "text.5093ead90fce", "text.f57beb90828a", "text.649eace2ae87",
        "text.08997b56437a", "document.observedSource", "document.observedDate", "config.notes").map(i18n::text)

    fun values(feed: PowerFeed, index: ProjectIndex, i18n: Messages) = listOf(
        index.deviceName(feed.deviceId, i18n = i18n), feed.feedName, feed.feedType.toDisplayString(i18n),
        index.project.powerFeeds.firstOrNull { it.id == feed.id }?.sourceDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: "-", feed.sourceOutletDescription ?: "-",
        feed.voltageVolts?.toString() ?: "-", feed.loadVa?.toString() ?: "-", feed.loadWatts?.toString() ?: "-",
        feed.observedRuntimeMinutes?.toString() ?: "-", feed.observedSource ?: "-",
        feed.observedEpochMs?.let { SimpleDateFormat("dd/MM/yyyy HH:mm", i18n.locale).format(Date(it)) } ?: "-", feed.notes ?: "-")
}
