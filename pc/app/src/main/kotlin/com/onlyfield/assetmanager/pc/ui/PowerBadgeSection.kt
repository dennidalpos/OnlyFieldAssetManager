package com.onlyfield.assetmanager.pc.ui

import com.onlyfield.assetmanager.pc.LocalMessages

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onlyfield.assetmanager.core.display.ProjectIndex
import com.onlyfield.assetmanager.core.display.toDisplayString
import com.onlyfield.assetmanager.core.model.*
import com.onlyfield.assetmanager.core.edit.ProjectEdits
import com.onlyfield.assetmanager.pc.ui.components.*
import com.onlyfield.assetmanager.core.forms.BadgeForm
import com.onlyfield.assetmanager.core.forms.PoeForm
import com.onlyfield.assetmanager.core.forms.PowerFeedForm

@Composable
fun PowerBadgeSection(project: Project, onProjectUpdated: (Project, String) -> Unit) {
    val i18n = LocalMessages.current

    val index = remember(project) { ProjectIndex(project) }
    var tab by remember { mutableStateOf(0) }
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SubTabs(
            listOf(i18n.text("text.370b792df123", project.powerFeeds.size), i18n.text("text.ffaf43588488", project.poeMappings.size), i18n.text("text.605ad6ef80b0", project.documentBadges.size)),
            tab
        ) { tab = it }
        when (tab) {
            0 -> FeedsTab(project, index, onProjectUpdated)
            1 -> PoeTab(project, index, onProjectUpdated)
            2 -> BadgesTab(project, index, onProjectUpdated)
        }
    }
}

@Composable
private fun FeedsTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    val i18n = LocalMessages.current

    var query by remember { mutableStateOf("") }
    var editing by remember { mutableStateOf<PowerFeed?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }
    val feeds = project.powerFeeds.filter { matchesQuery(query, it.feedName, index.deviceName(it.deviceId, i18n = i18n), index.deviceName(it.sourceDeviceId, "", i18n = i18n)) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.21adae0b690e"), searchQuery = query, onSearchChange = { query = it }, searchPlaceholder = i18n.text("text.2abb86128114")) {
            Button(onClick = { changeDetail { creating = true } }, enabled = index.devices.isNotEmpty()) { Text(i18n.text("text.04585136780b")) }
        }
        if (feeds.isEmpty()) EmptyState(if (index.devices.isEmpty()) i18n.text("text.0b91a2f27a41") else i18n.text("text.90bbe6100ed6"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(feeds, key = { it.id }) { f ->
                val source = f.sourceDeviceId?.let { index.deviceName(it, i18n = i18n) } ?: f.sourceOutletDescription
                ItemCard(
                    title = "${index.deviceName(f.deviceId, i18n.text("text.befe1ad89357"), i18n = i18n)} · ${f.feedName}",
                    badge = f.feedType.toDisplayString(i18n = i18n),
                    details = listOf(
                        source?.let { i18n.text("text.4bc711ab248c", it) }.orEmpty(),
                        listOfNotNull(
                            f.voltageVolts?.let { i18n.text("text.edb2066c2a30", it) },
                            f.loadWatts?.let { i18n.text("text.cd49315c743a", trim(it)) },
                            f.loadVa?.let { i18n.text("text.1df071e9be6a", trim(it)) },
                            f.observedRuntimeMinutes?.let { i18n.text("text.2dc280aa0f83", it) }
                        ).joinToString(" · "),
                        f.notes.orEmpty()
                    )
                ) {
                    EditButton { editing = f }
                    DeleteButton(f.feedName, onDelete = { onProjectUpdated(ProjectEdits.deletePowerFeed(project, f.id), i18n.text("text.a5c30e639a4d")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val f = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, f) { mutableStateOf(PowerFeedForm.from(f)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (f == null) i18n.text("text.4caa90bd8cd8") else i18n.text("text.ef9307d35ec6"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toFeed(f)
                creating = false; editing = null
                onProjectUpdated(if (f == null) ProjectEdits.addPowerFeed(project, saved) else ProjectEdits.updatePowerFeed(project, saved), i18n.text("text.4237371feb41"))
            },
            width = 640.dp
        ) {
            DevicePicker(i18n.text("text.a814bcd8ca15"), index, form.deviceId, { form = form.copy(deviceId = it) }, error = errors["deviceId"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.feedName, { form = form.copy(feedName = it) }, i18n.text("text.694885c1179e"), Modifier.weight(1.3f), errors["feedName"], hint = i18n.text("text.eb9a6bbaecd2"))
                EnumPicker(i18n.text("text.3868d2843d59"), PowerFeedType.entries, form.feedType, { it.toDisplayString(i18n = i18n) }, { form = form.copy(feedType = it) }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DevicePicker(i18n.text("text.11ae92f057eb"), index, form.sourceDeviceId, { form = form.copy(sourceDeviceId = it) }, Modifier.weight(1f),
                    noneLabel = i18n.text("text.65389ba5d2fd"), error = errors["sourceDeviceId"])
                FormField(form.sourceOutlet, { form = form.copy(sourceOutlet = it) }, i18n.text("text.cd33696ca977"), Modifier.weight(1f), hint = i18n.text("text.e8d9aee3669c"))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.voltage, { form = form.copy(voltage = it) }, i18n.text("text.5093ead90fce"), Modifier.weight(1f), errors["voltage"])
                FormField(form.loadWatts, { form = form.copy(loadWatts = it) }, i18n.text("text.eb98296d7970"), Modifier.weight(1f), errors["loadWatts"])
                FormField(form.loadVa, { form = form.copy(loadVa = it) }, i18n.text("text.e821b548ca4b"), Modifier.weight(1f), errors["loadVa"])
                FormField(form.runtimeMinutes, { form = form.copy(runtimeMinutes = it) }, i18n.text("text.08997b56437a"), Modifier.weight(1f), errors["runtimeMinutes"])
            }
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"), singleLine = false)
        }
    }
}

@Composable
private fun PoeTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<PoeMapping?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.b4f1378ac2dd"), subtitle = i18n.text("text.07b3cd2501fb")) {
            Button(onClick = { changeDetail { creating = true } }, enabled = index.ports.isNotEmpty()) { Text(i18n.text("text.b5aeb95142a5")) }
        }
        if (project.poeMappings.isEmpty()) EmptyState(if (index.ports.isEmpty()) i18n.text("text.411098441beb") else i18n.text("text.0c1e677a5476"))
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.poeMappings, key = { it.id }) { poe ->
                ItemCard(
                    title = index.portLabel(poe.portId, i18n.text("text.3ee4399caf53")),
                    badge = poe.role.toDisplayString(i18n = i18n),
                    details = listOf(listOfNotNull(poe.standard.toDisplayString(i18n = i18n), poe.allocatedPowerWatts?.let { i18n.text("text.cd49315c743a", trim(it)) }).joinToString(" · "), poe.notes.orEmpty())
                ) {
                    EditButton { editing = poe }
                    DeleteButton(i18n.text("text.3685af18e07a", index.portLabel(poe.portId)), onDelete = { onProjectUpdated(ProjectEdits.deletePoeMapping(project, poe.id), i18n.text("text.4e9f5d77e309")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val poe = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, poe) { mutableStateOf(PoeForm.from(poe)) }
        val errors = form.errors(i18n = i18n)
        val existingOnPort = project.poeMappings.find { it.portId == form.portId && it.id != poe?.id }
        EditPanel(
            title = if (poe == null) i18n.text("text.129c22f2302d") else i18n.text("text.a1a41a98a32c"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toMapping(poe)
                creating = false; editing = null
                onProjectUpdated(ProjectEdits.addOrUpdatePoeMapping(project, saved), i18n.text("text.f9730fd8963f"))
            },
            width = 600.dp
        ) {
            PortPicker(i18n.text("text.57c2ec879203"), index, form.portId, { form = form.copy(portId = it) }, noneLabel = null, error = errors["portId"])
            if (existingOnPort != null) Text(i18n.text("text.2d099536dd33"), color = MaterialTheme.colorScheme.tertiary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                EnumPicker(i18n.text("text.7a972bbc1480"), PoeRole.entries, form.role, { it.toDisplayString(i18n = i18n) }, { form = form.copy(role = it) }, Modifier.weight(1f))
                EnumPicker(i18n.text("text.ef6691545d2c"), PoeStandard.entries, form.standard, { it.toDisplayString(i18n = i18n) }, { form = form.copy(standard = it) }, Modifier.weight(1f))
            }
            FormField(form.watts, { form = form.copy(watts = it) }, i18n.text("text.548f9030240c"), error = errors["watts"])
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"))
        }
    }
}

@Composable
private fun BadgesTab(project: Project, index: ProjectIndex, onProjectUpdated: (Project, String) -> Unit) {
    val i18n = LocalMessages.current

    var editing by remember { mutableStateOf<DocumentBadge?>(null) }
    val changeDetail = LocalDetailChange.current
    var creating by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(i18n.text("text.d3154f7a9686"), subtitle = i18n.text("text.c965e24a18f4")) {
            Button(onClick = { changeDetail { creating = true } }) { Text(i18n.text("text.5e9867b48f83")) }
        }
        if (project.documentBadges.isEmpty()) EmptyState(i18n.text("text.eb3e34b0bacb"), actionLabel = i18n.text("text.5e9867b48f83"), onAction = { creating = true })
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(project.documentBadges, key = { it.id }) { b ->
                ItemCard(
                    title = b.label,
                    badge = b.category.toDisplayString(i18n = i18n),
                    details = listOf(index.targetLabel(b.targetType, b.targetId, i18n = i18n), if (b.isDerived) i18n.text("text.1e564953491e") else "", b.notes.orEmpty())
                ) {
                    if (!b.isDerived) EditButton { editing = b }
                    DeleteButton(b.label, onDelete = { onProjectUpdated(ProjectEdits.deleteDocumentBadge(project, b.id), i18n.text("text.fc7c2dba7445")) })
                }
            }
        }
    }

    if (creating || editing != null) {
        val b = editing
        var form by remember(LocalDetailSlot.current?.editorVersion, b) { mutableStateOf(BadgeForm.from(b)) }
        val errors = form.errors(i18n = i18n)
        EditPanel(
            title = if (b == null) i18n.text("text.66060a3a6be2") else i18n.text("text.6720d0dc04c9"),
            onDismiss = { creating = false; editing = null },
            confirmEnabled = errors.isEmpty(),
            onConfirm = {
                val saved = form.toBadge(b, project.id)
                creating = false; editing = null
                onProjectUpdated(if (b == null) ProjectEdits.addDocumentBadge(project, saved) else ProjectEdits.updateDocumentBadge(project, saved), i18n.text("text.6b86c681c041"))
            },
            width = 620.dp
        ) {
            TargetPicker(index, form.target, { form = form.copy(target = it) }, errors["target"])
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FormField(form.label, { form = form.copy(label = it) }, i18n.text("text.77a1b70aa654"), Modifier.weight(1.3f), errors["label"])
                EnumPicker(i18n.text("text.54276aa0307f"), BadgeCategory.entries, form.category, { it.toDisplayString(i18n = i18n) }, { form = form.copy(category = it) }, Modifier.weight(1f))
            }
            FormField(form.notes, { form = form.copy(notes = it) }, i18n.text("text.d8da2c49df39"))
        }
    }
}

private fun trim(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else value.toString().replace('.', ',')
