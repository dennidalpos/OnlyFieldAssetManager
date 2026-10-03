package com.onlyfield.assetmanager.data.repository

import androidx.room.withTransaction
import com.onlyfield.assetmanager.core.model.Device
import com.onlyfield.assetmanager.core.model.DeviceModel
import com.onlyfield.assetmanager.core.model.Project
import com.onlyfield.assetmanager.core.model.Rack
import com.onlyfield.assetmanager.data.local.AppDatabase
import com.onlyfield.assetmanager.data.local.AreaEntity
import com.onlyfield.assetmanager.data.local.BusinessUnitEntity
import com.onlyfield.assetmanager.data.local.CredentialEntity
import com.onlyfield.assetmanager.exchange.AttachmentFiles
import com.onlyfield.assetmanager.data.local.DeviceEntity
import com.onlyfield.assetmanager.data.local.DeviceModelEntity
import com.onlyfield.assetmanager.data.local.PortEntity
import android.content.Context
import android.print.PrintManager
import com.onlyfield.assetmanager.core.model.ExportFilterConfig
import com.onlyfield.assetmanager.core.model.ReportSelection
import com.onlyfield.assetmanager.data.local.ProjectEntity
import com.onlyfield.assetmanager.data.local.RackEntity
import com.onlyfield.assetmanager.data.local.SiteEntity
import com.onlyfield.assetmanager.exchange.DeviceModelSerializer
import com.onlyfield.assetmanager.exchange.MarkdownExportManager
import com.onlyfield.assetmanager.exchange.PackageImportResult
import com.onlyfield.assetmanager.exchange.PackageSerializer
import com.onlyfield.assetmanager.exchange.PasswordHasher
import com.onlyfield.assetmanager.exchange.ProjectComparison
import com.onlyfield.assetmanager.exchange.ProjectComparisonEvaluator
import com.onlyfield.assetmanager.exchange.ProjectPackage
import com.onlyfield.assetmanager.exchange.XlsxExportManager
import com.onlyfield.assetmanager.export.PdfExportManager
import com.onlyfield.assetmanager.export.ProjectPrintDocumentAdapter
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream
import kotlinx.coroutines.Dispatchers

/** Follows cables and panel mappings from a port, step by step. */
internal object CableTracer {
    fun trace(project: Project, startPortId: String): List<ChainStep> {
        val steps = mutableListOf<ChainStep>()
        val visitedPortIds = mutableSetOf<String>()

        val allPortsMap = mutableMapOf<String, Pair<com.onlyfield.assetmanager.core.model.Port, Device>>()
        for (bu in project.businessUnits) {
            for (dev in bu.devices) {
                for (port in dev.ports) {
                    allPortsMap[port.id] = port to dev
                }
            }
        }

        var currentPortId: String? = startPortId
        var stepIndex = 1

        while (currentPortId != null && !visitedPortIds.contains(currentPortId)) {
            visitedPortIds.add(currentPortId)

            val (currentPort, currentDevice) = allPortsMap[currentPortId] ?: Pair(null, null)

            // Look for a cable connected to currentPortId
            val cable = project.cables.find { it.portAId == currentPortId || it.portBId == currentPortId }

            if (cable != null) {
                val nextPortId = if (cable.portAId == currentPortId) cable.portBId else cable.portAId
                val (nextPort, nextDevice) = nextPortId?.let { allPortsMap[it] } ?: Pair(null, null)

                val sharedPathNames = cable.sharedPathSegmentIds.mapNotNull { segId ->
                    project.sharedPathSegments.find { it.id == segId }?.name
                }
                val pathInfo = if (sharedPathNames.isNotEmpty()) " [Percorso: ${sharedPathNames.joinToString(", ")}]" else ""
                val orientInfo = if (cable.orientation != com.onlyfield.assetmanager.core.model.CableOrientation.NONE) " [Orientamento: ${cable.orientation}]" else ""

                val desc = if (nextPort != null && nextDevice != null) {
                    "Cavo '${cable.codeOrLabel ?: "Senza Etichetta"}' (${cable.medium}, ${cable.observedSpeed ?: "velocità N/D"})$pathInfo$orientInfo -> Porta '${nextPort.name}' su '${nextDevice.technicalName}'"
                } else {
                    "Cavo '${cable.codeOrLabel ?: "Senza Etichetta"}' (${cable.medium})$pathInfo$orientInfo -> Estremità scollegata / da verificare"
                }

                steps.add(
                    ChainStep(
                        stepIndex = stepIndex++,
                        currentPort = currentPort,
                        currentDevice = currentDevice,
                        cable = cable,
                        panelMapping = null,
                        isUnknownPassage = nextPortId == null,
                        description = desc
                    )
                )

                if (nextPortId == null) {
                    break
                }

                currentPortId = nextPortId

                val mapping = project.panelMappings.find { it.portAId == currentPortId || it.portBId == currentPortId }
                if (mapping != null) {
                    if (mapping.isUnknownPassage || mapping.portBId == null) {
                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = nextPort,
                                currentDevice = nextDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = true,
                                description = "Passaggio ignoto / Ponte intermedio su '${nextDevice?.technicalName ?: "Pannello"}'"
                            )
                        )
                        break
                    } else {
                        val mappedPortId = if (mapping.portAId == currentPortId) mapping.portBId else mapping.portAId
                        val (mappedPort, mappedDevice) = mappedPortId?.let { allPortsMap[it] } ?: Pair(null, null)

                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = nextPort,
                                currentDevice = nextDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = false,
                                description = "Mapping Pannello (${mapping.mappingType}) -> Porta '${mappedPort?.name ?: "N/D"}' su '${mappedDevice?.technicalName ?: "Pannello"}'"
                            )
                        )

                        if (mappedPortId != null && !visitedPortIds.contains(mappedPortId)) {
                            currentPortId = mappedPortId
                        } else {
                            break
                        }
                    }
                } else {
                    val nextCable = project.cables.find { (it.portAId == currentPortId || it.portBId == currentPortId) && it.id != cable.id }
                    if (nextCable == null) {
                        break
                    }
                }
            } else {
                val mapping = project.panelMappings.find { it.portAId == currentPortId || it.portBId == currentPortId }
                if (mapping != null) {
                    if (mapping.isUnknownPassage || mapping.portBId == null) {
                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = currentPort,
                                currentDevice = currentDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = true,
                                description = "Passaggio ignoto / Ponte intermedio su '${currentDevice?.technicalName ?: "Pannello"}'"
                            )
                        )
                        break
                    } else {
                        val mappedPortId = if (mapping.portAId == currentPortId) mapping.portBId else mapping.portAId
                        val (mappedPort, mappedDevice) = mappedPortId?.let { allPortsMap[it] } ?: Pair(null, null)

                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = currentPort,
                                currentDevice = currentDevice,
                                cable = null,
                                panelMapping = mapping,
                                isUnknownPassage = false,
                                description = "Mapping Pannello (${mapping.mappingType}) -> Porta '${mappedPort?.name ?: "N/D"}' su '${mappedDevice?.technicalName ?: "Pannello"}'"
                            )
                        )

                        if (mappedPortId != null && !visitedPortIds.contains(mappedPortId)) {
                            currentPortId = mappedPortId
                        } else {
                            break
                        }
                    }
                } else {
                    if (steps.isEmpty()) {
                        steps.add(
                            ChainStep(
                                stepIndex = stepIndex++,
                                currentPort = currentPort,
                                currentDevice = currentDevice,
                                cable = null,
                                panelMapping = null,
                                isUnknownPassage = false,
                                description = "Porta '${currentPort?.name}' su '${currentDevice?.technicalName}' (nessun cavo collegato)"
                            )
                        )
                    }
                    break
                }
            }
        }

        return steps
    }
}

data class ChainStep(
    val stepIndex: Int,
    val currentPort: com.onlyfield.assetmanager.core.model.Port?,
    val currentDevice: Device?,
    val cable: com.onlyfield.assetmanager.core.model.Cable?,
    val panelMapping: com.onlyfield.assetmanager.core.model.PanelMapping?,
    val isUnknownPassage: Boolean = false,
    val description: String,
)
