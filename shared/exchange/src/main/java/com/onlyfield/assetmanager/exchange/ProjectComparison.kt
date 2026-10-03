package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.model.Project
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ComparisonStatus {
    IDENTICAL,
    NEWER_REVISION,
    OLDER_REVISION,
    DIVERGENT,
    DIFFERENT_PROJECT
}

data class ProjectComparison(
    val status: ComparisonStatus,
    val currentProjectId: String?,
    val currentProjectName: String?,
    val currentExportedEpochMs: Long?,
    val currentUpdatedEpochMs: Long?,
    val incomingProjectId: String,
    val incomingProjectName: String,
    val incomingExportedEpochMs: Long,
    val incomingUpdatedEpochMs: Long,
    val summary: String,
    val warningMessage: String? = null,
)

object ProjectComparisonEvaluator {

    fun evaluate(
        currentProject: Project?,
        currentManifest: PackageManifest? = null,
        incomingPackage: ProjectPackage,
    ): ProjectComparison {
        val (incomingManifest, incomingProj) = incomingPackage

        if (currentProject == null) {
            return ProjectComparison(
                status = ComparisonStatus.NEWER_REVISION,
                currentProjectId = null,
                currentProjectName = null,
                currentExportedEpochMs = null,
                currentUpdatedEpochMs = null,
                incomingProjectId = incomingProj.id,
                incomingProjectName = incomingProj.name,
                incomingExportedEpochMs = incomingManifest.exportedEpochMs,
                incomingUpdatedEpochMs = incomingProj.updatedEpochMs,
                summary = "Nuovo progetto locale",
            )
        }

        if (currentProject.id != incomingProj.id) {
            return ProjectComparison(
                status = ComparisonStatus.DIFFERENT_PROJECT,
                currentProjectId = currentProject.id,
                currentProjectName = currentProject.name,
                currentExportedEpochMs = currentManifest?.exportedEpochMs,
                currentUpdatedEpochMs = currentProject.updatedEpochMs,
                incomingProjectId = incomingProj.id,
                incomingProjectName = incomingProj.name,
                incomingExportedEpochMs = incomingManifest.exportedEpochMs,
                incomingUpdatedEpochMs = incomingProj.updatedEpochMs,
                summary = "Progetto con identificatore differente rispetto a quello aperto",
                warningMessage = "Attenzione: Stai importando un progetto differente (${incomingProj.name}) che affiancherà o sostituirà la selezione attuale.",
            )
        }

        // Check if project content is semantically identical
        val isIdenticalContent = com.onlyfield.assetmanager.core.model.ObjectHierarchy.normalize(currentProject) ==
            com.onlyfield.assetmanager.core.model.ObjectHierarchy.normalize(incomingProj)
        val currentExportEpoch = currentManifest?.exportedEpochMs ?: currentProject.updatedEpochMs

        if (isIdenticalContent && ((currentManifest == null) || (currentManifest.exportId == incomingManifest.exportId))) {
            return ProjectComparison(
                status = ComparisonStatus.IDENTICAL,
                currentProjectId = currentProject.id,
                currentProjectName = currentProject.name,
                currentExportedEpochMs = currentExportEpoch,
                currentUpdatedEpochMs = currentProject.updatedEpochMs,
                incomingProjectId = incomingProj.id,
                incomingProjectName = incomingProj.name,
                incomingExportedEpochMs = incomingManifest.exportedEpochMs,
                incomingUpdatedEpochMs = incomingProj.updatedEpochMs,
                summary = "La copia importata è identica a quella locale",
            )
        }

        if ((incomingManifest.exportedEpochMs > currentExportEpoch) && (incomingProj.updatedEpochMs >= currentProject.updatedEpochMs)) {
            return ProjectComparison(
                status = ComparisonStatus.NEWER_REVISION,
                currentProjectId = currentProject.id,
                currentProjectName = currentProject.name,
                currentExportedEpochMs = currentExportEpoch,
                currentUpdatedEpochMs = currentProject.updatedEpochMs,
                incomingProjectId = incomingProj.id,
                incomingProjectName = incomingProj.name,
                incomingExportedEpochMs = incomingManifest.exportedEpochMs,
                incomingUpdatedEpochMs = incomingProj.updatedEpochMs,
                summary = "La copia importata è una revisione più recente",
            )
        }

        if ((incomingManifest.exportedEpochMs < currentExportEpoch) || (incomingProj.updatedEpochMs < currentProject.updatedEpochMs)) {
            val formattedLocal = formatDate(currentExportEpoch)
            val formattedIncoming = formatDate(incomingManifest.exportedEpochMs)
            return ProjectComparison(
                status = ComparisonStatus.OLDER_REVISION,
                currentProjectId = currentProject.id,
                currentProjectName = currentProject.name,
                currentExportedEpochMs = currentExportEpoch,
                currentUpdatedEpochMs = currentProject.updatedEpochMs,
                incomingProjectId = incomingProj.id,
                incomingProjectName = incomingProj.name,
                incomingExportedEpochMs = incomingManifest.exportedEpochMs,
                incomingUpdatedEpochMs = incomingProj.updatedEpochMs,
                summary = "La copia importata è antecedente a quella locale",
                warningMessage = "AVVISO: La copia importata ($formattedIncoming) è antecedente alla versione locale corrente ($formattedLocal). Continuare sovrascriverà le modifiche locali più recenti.",
            )
        }

        return ProjectComparison(
            status = ComparisonStatus.DIVERGENT,
            currentProjectId = currentProject.id,
            currentProjectName = currentProject.name,
            currentExportedEpochMs = currentExportEpoch,
            currentUpdatedEpochMs = currentProject.updatedEpochMs,
            incomingProjectId = incomingProj.id,
            incomingProjectName = incomingProj.name,
            incomingExportedEpochMs = incomingManifest.exportedEpochMs,
            incomingUpdatedEpochMs = incomingProj.updatedEpochMs,
            summary = "Le due copie sono divergenti",
            warningMessage = "AVVISO: Le modifiche nella copia importata divergono dallo stato locale. Procedendo verranno applicati i dati del pacchetto importato.",
        )
    }

    private fun formatDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(epochMs))
    }
}
