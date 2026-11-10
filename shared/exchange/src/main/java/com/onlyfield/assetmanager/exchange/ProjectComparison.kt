package com.onlyfield.assetmanager.exchange

import com.onlyfield.assetmanager.core.i18n.Messages

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
     i18n: Messages = Messages()): ProjectComparison {
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
                summary = i18n.text("text.c889142ab486"),
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
                summary = i18n.text("text.e47e726c1f3f"),
                warningMessage = i18n.text("text.279fd779e388", incomingProj.name),
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
                summary = i18n.text("text.5dc0bf387420"),
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
                summary = i18n.text("text.41250362f6f4"),
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
                summary = i18n.text("text.6dbac3913064"),
                warningMessage = i18n.text("text.bbb48cace21a", formattedIncoming, formattedLocal),
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
            summary = i18n.text("text.5f87354e83f0"),
            warningMessage = i18n.text("text.ca47acdad352"),
        )
    }

    private fun formatDate(epochMs: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date(epochMs))
    }
}
